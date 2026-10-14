package edu.hcmute.careersim.simulation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.storage.FileAssetService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliverableValidator {
    private final FileAssetService files;

    public void definition(Definition d) {
        if (d == null || d.format() == null) CoreValues.invalid("Deliverable definition required.");
        CoreValues.text(d.name(), 180);
        if (d.position() < 1) CoreValues.invalid("Invalid deliverable position.");
        var c = d.validationConfig();
        if (c == null || !CoreValues.schemaOne(c))
            CoreValues.invalid("Validation schemaVersion 1 required.");
        switch (d.format()) {
            case "TEXT" -> {
                CoreValues.object(c, Set.of("schemaVersion", "minLength", "maxLength"));
                if (!c.path("minLength").isIntegralNumber()
                        || !c.path("maxLength").isIntegralNumber()
                        || !c.path("maxLength").canConvertToInt()
                        || c.path("minLength").asInt() < 1
                        || c.path("maxLength").asInt() > 10000
                        || c.path("maxLength").asInt() < c.path("minLength").asInt())
                    CoreValues.invalid("Invalid text bounds.");
            }
            case "URL" -> {
                CoreValues.object(c, Set.of("schemaVersion", "schemes", "maxLength"));
                if (!c.path("maxLength").isIntegralNumber()
                        || !c.path("maxLength").canConvertToInt()
                        || !c.path("schemes").equals(CoreValues.tree(List.of("https")))
                        || c.path("maxLength").asInt() < 1
                        || c.path("maxLength").asInt() > 2000)
                    CoreValues.invalid("Invalid URL bounds.");
            }
            case "FILE" -> {
                CoreValues.object(c, Set.of("schemaVersion", "maxBytes", "allowedMimeTypes"));
                if (!c.path("maxBytes").isIntegralNumber()
                        || !c.path("maxBytes").canConvertToLong()
                        || c.path("maxBytes").asLong() < 1
                        || c.path("maxBytes").asLong() > 10485760
                        || !c.path("allowedMimeTypes").isArray()
                        || c.path("allowedMimeTypes").isEmpty())
                    CoreValues.invalid("Invalid file policy.");
                c.path("allowedMimeTypes")
                        .forEach(
                                m -> {
                                    if (!Set.of(
                                                    "text/plain",
                                                    "application/pdf",
                                                    "image/png",
                                                    "image/jpeg")
                                            .contains(m.asText()))
                                        CoreValues.invalid("Unsupported MIME type.");
                                });
            }
            case "JSON" -> {
                CoreValues.object(c, Set.of("schemaVersion", "jsonSchema"));
                schema(c.path("jsonSchema"), 0);
                if (!"object".equals(c.path("jsonSchema").path("type").asText()))
                    CoreValues.invalid("JSON deliverables require an object.");
            }
            default -> CoreValues.invalid("Unsupported deliverable format.");
        }
    }

    private void schema(JsonNode n, int depth) {
        if (depth > 8) CoreValues.invalid("JSON schema too deep.");
        CoreValues.object(
                n,
                Set.of(
                        "type",
                        "properties",
                        "items",
                        "enum",
                        "required",
                        "maxItems",
                        "maxLength",
                        "additionalProperties"));
        if (!Set.of("object", "array", "string", "integer").contains(n.path("type").asText()))
            CoreValues.invalid("Unsupported JSON schema type.");
        if (n.has("additionalProperties")
                && !n.path("additionalProperties").equals(CoreValues.tree(false)))
            CoreValues.invalid("Invalid additionalProperties.");
        if ("array".equals(n.path("type").asText()) && !n.has("items"))
            CoreValues.invalid("Array item schema required.");
        if (n.has("enum") && (!n.path("enum").isArray() || n.path("enum").size() > 100))
            CoreValues.invalid("Invalid enum.");
        if (n.has("maxItems")
                && (!n.path("maxItems").isIntegralNumber()
                        || !n.path("maxItems").canConvertToInt()
                        || n.path("maxItems").asInt() < 0
                        || n.path("maxItems").asInt() > 100))
            CoreValues.invalid("Invalid array bound.");
        if (n.has("maxLength")
                && (!n.path("maxLength").isIntegralNumber()
                        || !n.path("maxLength").canConvertToInt()
                        || n.path("maxLength").asInt() < 0
                        || n.path("maxLength").asInt() > 10000))
            CoreValues.invalid("Invalid string bound.");
        if (n.has("properties")) {
            if (!n.path("properties").isObject() || n.path("properties").size() > 100)
                CoreValues.invalid("Invalid properties.");
            n.path("properties").forEach(v -> schema(v, depth + 1));
        }
        if (n.has("items")) schema(n.path("items"), depth + 1);
        if (n.has("required")) {
            if (!n.path("required").isArray()) CoreValues.invalid("Invalid required fields.");
            n.path("required")
                    .forEach(
                            v -> {
                                if (!v.isTextual() || !n.path("properties").has(v.asText()))
                                    CoreValues.invalid("Unknown required field.");
                            });
        }
    }

    private void json(JsonNode n, JsonNode s, int depth) {
        if (depth > 8 || n == null || n.isNull()) CoreValues.invalid("Invalid JSON payload.");
        String t = s.path("type").asText();
        if ((t.equals("object") && !n.isObject())
                || (t.equals("array") && !n.isArray())
                || (t.equals("string") && !n.isTextual())
                || (t.equals("integer") && !n.isIntegralNumber()))
            CoreValues.invalid("JSON type mismatch.");
        if (s.has("enum")) {
            boolean found = false;
            for (var option : s.path("enum")) if (option.equals(n)) found = true;
            if (!found) CoreValues.invalid("Value is not a permitted option.");
        }
        if (n.isObject()) {
            n.fieldNames()
                    .forEachRemaining(
                            k -> {
                                if (!s.path("properties").has(k))
                                    CoreValues.invalid("Unknown payload field.");
                                json(n.get(k), s.path("properties").path(k), depth + 1);
                            });
            s.path("required")
                    .forEach(
                            k -> {
                                if (!n.has(k.asText()))
                                    CoreValues.invalid("Missing payload field.");
                            });
        }
        if (n.isArray()) {
            if (n.size() > s.path("maxItems").asInt(100))
                CoreValues.invalid("Array exceeds bound.");
            n.forEach(v -> json(v, s.path("items"), depth + 1));
        }
        if (n.isTextual()
                && (n.asText().length() > s.path("maxLength").asInt(10000)
                        || n.asText().indexOf('\0') >= 0))
            CoreValues.invalid("String exceeds bound.");
    }

    public List<Item> validate(
            long actor, List<Definition> definitions, List<Item> items, boolean finalizing) {
        if (items == null || items.size() > 100) CoreValues.invalid("Invalid deliverable set.");
        Map<String, Definition> all = new HashMap<>();
        definitions.forEach(d -> all.put(d.id(), d));
        Set<String> seen = new HashSet<>();
        List<Item> normalized = new ArrayList<>();
        for (var i : items) {
            if (i == null) CoreValues.invalid("Deliverable item required.");
            var d = all.get(i.definitionId());
            if (d == null || !seen.add(i.definitionId()))
                CoreValues.invalid("Unknown or duplicate definition.");
            int count =
                    (i.contentText() != null ? 1 : 0)
                            + (i.contentUrl() != null ? 1 : 0)
                            + (i.fileAssetId() != null ? 1 : 0)
                            + (i.contentJson() != null ? 1 : 0);
            if (count != 1) CoreValues.invalid("Exactly one deliverable payload is required.");
            var c = d.validationConfig();
            String text = i.contentText();
            switch (d.format()) {
                case "TEXT" -> {
                    if (text == null) CoreValues.invalid("Text payload required.");
                    text = CoreValues.text(text, c.path("maxLength").asInt());
                    if (text.strip().length() < c.path("minLength").asInt())
                        CoreValues.invalid("Text is too short.");
                }
                case "URL" -> {
                    if (i.contentUrl() == null) CoreValues.invalid("URL payload required.");
                    CoreValues.https(i.contentUrl());
                    if (i.contentUrl().length() > c.path("maxLength").asInt())
                        CoreValues.invalid("URL too long.");
                }
                case "FILE" -> {
                    if (i.fileAssetId() == null) CoreValues.invalid("File payload required.");
                    var f = files.requireReady(actor, CoreValues.id(i.fileAssetId()), null);
                    boolean allowed = false;
                    for (var mime : c.path("allowedMimeTypes"))
                        if (mime.asText().equals(f.mimeType())) allowed = true;
                    if (!allowed || f.sizeBytes() > c.path("maxBytes").asLong())
                        CoreValues.invalid("File does not match definition.");
                }
                case "JSON" -> {
                    if (i.contentJson() == null
                            || !i.contentJson().isObject()
                            || CoreValues.write(i.contentJson()).length() > 10000)
                        CoreValues.invalid("Bounded JSON object required.");
                    json(i.contentJson(), c.path("jsonSchema"), 0);
                }
                default -> CoreValues.invalid("Unknown format.");
            }
            normalized.add(
                    new Item(
                            i.definitionId(),
                            text,
                            i.contentUrl(),
                            i.fileAssetId(),
                            i.contentJson()));
        }
        if (finalizing) {
            Map<String, String> missing = new LinkedHashMap<>();
            definitions.stream()
                    .filter(Definition::required)
                    .filter(d -> !seen.contains(d.id()))
                    .forEach(
                            d ->
                                    missing.put(
                                            "items." + d.id(),
                                            "A required deliverable is missing."));
            if (!missing.isEmpty())
                throw new edu.hcmute.careersim.common.exception.DomainValidationException(
                        "All required deliverables must be supplied.", missing);
        }
        return normalized.stream()
                .sorted(Comparator.comparingLong(i -> CoreValues.id(i.definitionId())))
                .toList();
    }

    public String hash(long actor, long enrollment, long task, long version, List<Item> items) {
        List<Object> payload = new ArrayList<>();
        for (var i : items) {
            String checksum =
                    i.fileAssetId() == null
                            ? ""
                            : files.requireReady(actor, CoreValues.id(i.fileAssetId()), null)
                                    .checksum();
            payload.add(Arrays.asList(i, checksum));
        }
        return CoreValues.hash(
                Map.of(
                        "enrollmentId",
                        Long.toString(enrollment),
                        "taskId",
                        Long.toString(task),
                        "versionId",
                        Long.toString(version),
                        "items",
                        payload));
    }
}
