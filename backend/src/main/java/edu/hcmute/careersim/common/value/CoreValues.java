package edu.hcmute.careersim.common.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Representation primitives only. Domain-specific schemas belong to their modules. */
public final class CoreValues {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private CoreValues() {}

    public static boolean schemaOne(JsonNode node) {
        return node != null
                && node.path("schemaVersion").isIntegralNumber()
                && node.path("schemaVersion").canConvertToInt()
                && node.path("schemaVersion").intValue() == 1;
    }

    public static String text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max || value.indexOf('\0') >= 0)
            invalid("Invalid text value.");
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    public static long id(String value) {
        try {
            if (value == null || !value.matches("[1-9][0-9]{0,18}"))
                throw new IllegalArgumentException();
            return Long.parseLong(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.DOMAIN_VALIDATION, "Invalid decimal ID.");
        }
    }

    public static long revision(String header) {
        if (header == null) throw new ApiException(ErrorCode.PRECONDITION_REQUIRED);
        try {
            if (!header.matches("(?:[0-9]+|\"[0-9]+\")")) throw new IllegalArgumentException();
            long r = Long.parseLong(header.replace("\"", ""));
            if (r < 0) throw new IllegalArgumentException();
            return r;
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.DOMAIN_VALIDATION, "Invalid revision.");
        }
    }

    public static void match(long expected, long actual) {
        if (expected != actual)
            throw new ApiException(ErrorCode.CONFLICT, "The draft changed. Reload before saving.");
    }

    public static void invalid(String message) {
        throw new ApiException(ErrorCode.DOMAIN_VALIDATION, message);
    }

    public static JsonNode json(String value) {
        try {
            return JSON.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Stored value is invalid", e);
        }
    }

    public static JsonNode tree(Object value) {
        return JSON.valueToTree(value);
    }

    public static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON value", e);
        }
    }

    public static String hash(Object value) {
        return bytesHash(write(canonical(tree(value))).getBytes(StandardCharsets.UTF_8));
    }

    public static String bytesHash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode canonical(JsonNode n) {
        if (n.isObject()) {
            ObjectNode o = JSON.createObjectNode();
            List<String> keys = new ArrayList<>();
            n.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            keys.forEach(k -> o.set(k, canonical(n.get(k))));
            return o;
        }
        if (n.isArray()) {
            ArrayNode a = JSON.createArrayNode();
            n.forEach(v -> a.add(canonical(v)));
            return a;
        }
        if (n.isTextual())
            return JSON.getNodeFactory()
                    .textNode(n.textValue().replace("\r\n", "\n").replace('\r', '\n'));
        return n;
    }

    public static void object(JsonNode node, Set<String> allowed) {
        if (node == null
                || !node.isObject()
                || write(node).getBytes(StandardCharsets.UTF_8).length > 32768)
            invalid("Expected bounded JSON object.");
        node.fieldNames()
                .forEachRemaining(
                        k -> {
                            if (!allowed.contains(k)) invalid("Unknown field: " + k);
                        });
    }

    public static void https(String value) {
        try {
            var u = java.net.URI.create(text(value, 2000));
            if (!"https".equals(u.getScheme())
                    || u.getHost() == null
                    || u.getUserInfo() != null
                    || u.getFragment() != null) invalid("Expected an HTTPS reference.");
        } catch (IllegalArgumentException e) {
            invalid("Invalid URL reference.");
        }
    }
}
