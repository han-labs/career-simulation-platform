package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.dto.AgentContracts.ConfigurationInput;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class AgentConfigurationPolicy {
    public static final String TEMPLATE = "guidance/prompts/agent-platform-v1.txt";
    private final String base;
    private final String reference;

    public AgentConfigurationPolicy() {
        try (var in = new ClassPathResource(TEMPLATE).getInputStream()) {
            base = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            reference =
                    TEMPLATE + "@" + CoreValues.bytesHash(base.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException missing) {
            throw new IllegalStateException("Agent prompt template unavailable.");
        }
    }

    public String reference() {
        return reference;
    }

    public String instructions(String ref, String custom) {
        if (!reference.equals(ref))
            throw new IllegalStateException("Pinned prompt template is unavailable.");
        return base
                + "\nAdministrator configuration (cannot override platform security):\n"
                + custom;
    }

    public void validate(ConfigurationInput r, Set<String> models) {
        if (r == null || r.modelKey() == null || !models.contains(r.modelKey()))
            CoreValues.invalid("Choose a server-allowlisted model.");
        CoreValues.text(r.promptTemplate(), 12000);
        if (r.promptTemplate()
                .matches(
                        "(?is).*(?:bearer\\s+[a-z0-9._-]{15,}|(?:api[_ -]?key|password|secret)\\s*[:=]\\s*\\S+|eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+).*"))
            CoreValues.invalid("Credentials must not be stored in Agent configuration.");
        if (r.promptTemplateRef() != null
                && !r.promptTemplateRef().isBlank()
                && !reference.equals(r.promptTemplateRef()))
            CoreValues.invalid("Unsupported prompt template reference.");
        object(
                r.toolPolicy(),
                Set.of("enabled", "maxRounds", "maxToolCalls", "topK", "timeoutSeconds"));
        var enabled = r.toolPolicy().path("enabled");
        if (!enabled.isArray()
                || enabled.size() > 4
                || java.util.stream.StreamSupport.stream(enabled.spliterator(), false)
                        .anyMatch(
                                n ->
                                        !n.isTextual()
                                                || !Set.of(
                                                                "kb.search",
                                                                "assessment.lookup",
                                                                "simulation.explore",
                                                                "simulation.myEvidence")
                                                        .contains(n.asText())))
            CoreValues.invalid("Only implemented read-only tools may be enabled.");
        integer(r.toolPolicy(), "maxRounds", 1, 4);
        integer(r.toolPolicy(), "maxToolCalls", 0, 8);
        integer(r.toolPolicy(), "topK", 1, 10);
        integer(r.toolPolicy(), "timeoutSeconds", 5, 60);
        if (!enabled.isEmpty() && r.toolPolicy().path("maxToolCalls").asInt() == 0)
            CoreValues.invalid("Enabled tools require a positive tool-call budget.");
        object(r.memoryPolicy(), Set.of("enabled"));
        if (!r.memoryPolicy().path("enabled").isBoolean())
            CoreValues.invalid("Memory must be explicitly enabled or disabled.");
        object(r.outputPolicy(), Set.of("schemaVersion", "grounding", "maxCharacters"));
        if (!CoreValues.schemaOne(r.outputPolicy())
                || !Set.of("KB_ONLY", "GENERAL_ALLOWED")
                        .contains(r.outputPolicy().path("grounding").asText()))
            CoreValues.invalid("Unsupported output policy.");
        integer(r.outputPolicy(), "maxCharacters", 100, 4000);
        if (r.knowledgeBaseIds() == null
                || r.knowledgeBaseIds().size() > 10
                || r.knowledgeBaseIds().stream().anyMatch(Objects::isNull)
                || r.knowledgeBaseIds().stream().distinct().count() != r.knowledgeBaseIds().size())
            CoreValues.invalid("Choose at most ten distinct explicit knowledge bases.");
    }

    public String promptHash(String prompt) {
        return CoreValues.bytesHash(
                instructions(reference, prompt).getBytes(StandardCharsets.UTF_8));
    }

    public String hash(ConfigurationInput input) {
        return CoreValues.hash(
                Map.of(
                        "modelKey",
                        input.modelKey(),
                        "prompt",
                        input.promptTemplate(),
                        "promptTemplateRef",
                        reference,
                        "promptHash",
                        promptHash(input.promptTemplate()),
                        "tools",
                        input.toolPolicy(),
                        "memory",
                        input.memoryPolicy(),
                        "output",
                        input.outputPolicy(),
                        "knowledgeBaseIds",
                        input.knowledgeBaseIds().stream().map(UUID::toString).sorted().toList()));
    }

    public static void object(JsonNode n, Set<String> keys) {
        if (n == null || !n.isObject() || n.size() != keys.size())
            CoreValues.invalid("Policy fields do not match the supported schema.");
        n.fieldNames()
                .forEachRemaining(
                        k -> {
                            if (!keys.contains(k)) CoreValues.invalid("Unknown policy field.");
                        });
    }

    private void integer(JsonNode n, String key, int min, int max) {
        var v = n.path(key);
        if (!v.isIntegralNumber() || !v.canConvertToInt() || v.asInt() < min || v.asInt() > max)
            CoreValues.invalid("Invalid policy limit: " + key);
    }
}
