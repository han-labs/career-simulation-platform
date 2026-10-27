package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import java.time.*;
import java.util.*;

/** Versioned, non-secret presentation policy. Never a data permission grant. */
public final class AgentExperience {
    private AgentExperience() {}

    public static void validate(JsonNode n, String instructions) {
        if (n == null || n.isNull()) return;
        var supported =
                new HashSet<>(
                        Set.of(
                                "schemaVersion",
                                "name",
                                "description",
                                "audience",
                                "language",
                                "greeting",
                                "timezone",
                                "variables",
                                "chat"));
        if (n.has("fallbackModelKeys")) supported.add("fallbackModelKeys");
        AgentConfigurationPolicy.object(n, supported);
        if (n.path("schemaVersion").asInt() != 2 || !n.path("schemaVersion").isIntegralNumber())
            CoreValues.invalid("Unsupported experience schema.");
        text(n, "name", 180, true);
        text(n, "description", 2000, true);
        text(n, "greeting", 1000, false);
        choice(n, "audience", Set.of("personal", "business"));
        choice(n, "language", Set.of("en", "vi", "bilingual"));
        if (!n.path("timezone").isTextual()
                || !ZoneId.getAvailableZoneIds().contains(n.path("timezone").asText()))
            CoreValues.invalid("Choose a valid IANA timezone.");
        variables(n);
        template(instructions, n);
        template(n.path("greeting").asText(), n);
        var chat = n.path("chat");
        AgentConfigurationPolicy.object(
                chat, Set.of("includeHistory", "maxTurns", "showSourceExcerpts", "responseStyle"));
        if (!chat.path("includeHistory").isBoolean()
                || !chat.path("showSourceExcerpts").isBoolean()
                || !chat.path("maxTurns").isIntegralNumber()
                || chat.path("maxTurns").asInt() < 1
                || chat.path("maxTurns").asInt() > 6) CoreValues.invalid("Invalid chat settings.");
        choice(chat, "responseStyle", Set.of("concise", "balanced", "detailed"));
    }

    private static void text(JsonNode n, String key, int max, boolean required) {
        if (!n.path(key).isTextual()
                || n.path(key).asText().length() > max
                || required && n.path(key).asText().isBlank())
            CoreValues.invalid("Invalid experience field: " + key);
        String value = n.path(key).asText();
        if (value.matches(
                "(?is).*(?:bearer\\s+[a-z0-9._-]{15,}|(?:api[_ -]?key|password|secret)\\s*[:=]\\s*\\S+|eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+).*"))
            CoreValues.invalid("Credentials must not be stored in Agent configuration.");
    }

    private static void choice(JsonNode n, String key, Set<String> values) {
        if (!n.path(key).isTextual() || !values.contains(n.path(key).asText()))
            CoreValues.invalid("Invalid experience field: " + key);
    }

    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("[{][{]([A-Za-z_][A-Za-z0-9_]{0,39})[}][}]");

    public static Map<String, String> variables(JsonNode n) {
        var result = new LinkedHashMap<String, String>();
        var vars = n.path("variables");
        if (vars.isObject())
            vars.fields().forEachRemaining(e -> variable(result, e.getKey(), e.getValue()));
        else if (vars.isArray()) {
            for (var v : vars) {
                AgentConfigurationPolicy.object(v, Set.of("key", "value"));
                if (!v.path("key").isTextual()) CoreValues.invalid("A variable key is required.");
                variable(result, v.path("key").asText(), v.path("value"));
            }
        } else CoreValues.invalid("Variables must be a bounded key/value collection.");
        if (result.size() > 20) CoreValues.invalid("Use at most 20 variables.");
        return result;
    }

    private static void variable(Map<String, String> values, String key, JsonNode value) {
        if (!key.matches("[A-Za-z_][A-Za-z0-9_]{0,39}")
                || Set.of("__proto__", "prototype", "constructor").contains(key)
                || key.matches("(?i).*(?:secret|password|token|api_?key|authorization|cookie).*"))
            CoreValues.invalid(
                    "Use a safe non-secret variable key (1-40 letters, digits or underscores).");
        if (values.containsKey(key)) CoreValues.invalid("Variable keys must be unique.");
        if (!value.isTextual()
                || value.asText().length() > 500
                || value.asText().codePoints().anyMatch(Character::isISOControl)
                || value.asText().contains("{{")
                || value.asText().contains("}}"))
            CoreValues.invalid(
                    "Variable values must be plain text, up to 500 characters, without nested templates.");
        text(CoreValues.tree(Map.of("value", value.asText())), "value", 500, false);
        values.put(key, value.asText());
    }

    private static void template(String value, JsonNode n) {
        var vars = variables(n);
        var matcher = PLACEHOLDER.matcher(value);
        while (matcher.find())
            if (!vars.containsKey(matcher.group(1)))
                CoreValues.invalid("Unknown variable: " + matcher.group(1));
        String rest = matcher.replaceAll("");
        if (rest.contains("{{") || rest.contains("}}"))
            CoreValues.invalid("Use {{key}} with a defined variable; expressions are unsupported.");
    }

    public static String render(String value, JsonNode n) {
        if (n == null || n.isNull()) return value;
        var vars = variables(n);
        var matcher = PLACEHOLDER.matcher(value);
        var out = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(
                    out,
                    java.util.regex.Matcher.quoteReplacement(
                            vars.getOrDefault(matcher.group(1), "")));
        matcher.appendTail(out);
        return out.toString();
    }

    public static String policy(JsonNode n) {
        if (n == null || n.isNull()) return "";
        return "\nVersioned Agent response policy (orientation never grants data access): "
                + (n.path("audience").asText().equals("business")
                        ? "Explain careers in workplace roles, processes and organizational skills. "
                        : "Support individual career exploration using authorized evidence. ")
                + switch (n.path("language").asText()) {
                    case "vi" -> "Reply in Vietnamese. ";
                    case "bilingual" -> "Reply with concise English and Vietnamese sections. ";
                    default -> "Reply in English. ";
                }
                + "This language policy takes precedence over conflicting custom language instructions. Response style: "
                + n.path("chat").path("responseStyle").asText()
                + "; preserve the output character limit. Timezone for interpreting dates: "
                + n.path("timezone").asText()
                + ".";
    }

    public static String at(JsonNode n, Instant started) {
        return n == null || n.isNull()
                ? ""
                : "\nExecution started at "
                        + started.atZone(ZoneId.of(n.path("timezone").asText()))
                        + " (backend timestamp, not a live clock tool).";
    }

    public static String greeting(JsonNode n) {
        if (n == null || n.isNull()) return "Hello. What would you like to explore?";
        String raw = n.path("greeting").asText();
        if (!raw.isBlank()) return render(raw, n);
        return switch (n.path("language").asText()) {
            case "vi" -> "Xin chào. Bạn muốn khám phá điều gì?";
            case "bilingual" ->
                    "Hello. What would you like to explore? / Xin chào. Bạn muốn khám phá điều gì?";
            default -> "Hello. What would you like to explore?";
        };
    }
}
