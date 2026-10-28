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
    public static final String TEMPLATE = "guidance/prompts/agent-platform-v2.txt";
    private final String base;
    private final String reference;
    private final String repairPolicy;
    private final String studentActions;
    private final Map<String, String> templates = new HashMap<>();

    public AgentConfigurationPolicy() {
        try (var in = new ClassPathResource(TEMPLATE).getInputStream()) {
            base = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            reference =
                    TEMPLATE + "@" + CoreValues.bytesHash(base.getBytes(StandardCharsets.UTF_8));
            templates.put(reference, base);
            try (var repair =
                    new ClassPathResource("guidance/prompts/agent-output-repair-v1.txt")
                            .getInputStream()) {
                repairPolicy = new String(repair.readAllBytes(), StandardCharsets.UTF_8);
            }
            try (var actions =
                    new ClassPathResource("guidance/prompts/student-actions-v4.txt")
                            .getInputStream()) {
                studentActions = new String(actions.readAllBytes(), StandardCharsets.UTF_8);
            }
            String legacy = "guidance/prompts/agent-platform-v1.txt";
            try (var old = new ClassPathResource(legacy).getInputStream()) {
                String content = new String(old.readAllBytes(), StandardCharsets.UTF_8);
                templates.put(
                        legacy
                                + "@"
                                + CoreValues.bytesHash(content.getBytes(StandardCharsets.UTF_8)),
                        content);
            }
        } catch (java.io.IOException missing) {
            throw new IllegalStateException("Agent prompt template unavailable.");
        }
    }

    public String reference() {
        return reference;
    }

    public String studentActionInstructions() {
        return studentActions;
    }

    public String repairInstructions() {
        return repairPolicy;
    }

    public String repairReference() {
        return "guidance/prompts/agent-output-repair-v1.txt@"
                + CoreValues.bytesHash(repairPolicy.getBytes(StandardCharsets.UTF_8));
    }

    public String instructions(String ref, String custom) {
        if (!templates.containsKey(ref))
            throw new IllegalStateException("Pinned prompt template is unavailable.");
        return templates.get(ref)
                + "\nAdministrator configuration (cannot override platform security):\n"
                + custom;
    }

    public void validate(ConfigurationInput r, Set<String> models) {
        if (r == null || r.modelKey() == null || !models.contains(r.modelKey()))
            CoreValues.invalid("Choose a server-allowlisted model.");
        CoreValues.text(r.promptTemplate(), 12000);
        AgentExperience.validate(r.experienceConfig(), r.promptTemplate());
        if (r.experienceConfig() != null && r.experienceConfig().has("fallbackModelKeys")) {
            var fallbacks = r.experienceConfig().path("fallbackModelKeys");
            var seen = new HashSet<String>();
            seen.add(r.modelKey());
            if (!fallbacks.isArray() || fallbacks.size() > 3)
                CoreValues.invalid("Choose at most three ordered fallback models.");
            String provider = r.modelKey().split("/", 2)[0] + "/";
            for (var fallback : fallbacks)
                if (!fallback.isTextual()
                        || !models.contains(fallback.asText())
                        || !fallback.asText().startsWith(provider)
                        || !seen.add(fallback.asText()))
                    CoreValues.invalid(
                            "Fallback models must be unique, compatible and use the primary provider.");
        }
        if (r.promptTemplate()
                .matches(
                        "(?is).*(?:bearer\\s+[a-z0-9._-]{15,}|(?:api[_ -]?key|password|secret)\\s*[:=]\\s*\\S+|eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+).*"))
            CoreValues.invalid("Credentials must not be stored in Agent configuration.");
        if (r.promptTemplateRef() != null
                && !r.promptTemplateRef().isBlank()
                && !templates.containsKey(r.promptTemplateRef()))
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
        if (input.experienceConfig() != null && !input.experienceConfig().isNull()) {
            return CoreValues.hash(
                    Map.of(
                            "schemaVersion",
                            2,
                            "legacyFields",
                            hash(
                                    new ConfigurationInput(
                                            input.modelKey(),
                                            input.promptTemplate(),
                                            input.promptTemplateRef(),
                                            input.toolPolicy(),
                                            input.memoryPolicy(),
                                            input.outputPolicy(),
                                            input.knowledgeBaseIds())),
                            "experience",
                            input.experienceConfig(),
                            "renderedPromptHash",
                            promptHash(input)));
        }
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

    public String instructions(String ref, String custom, JsonNode experience) {
        return instructions(ref, AgentExperience.render(custom, experience))
                + AgentExperience.policy(experience);
    }

    public String promptHash(ConfigurationInput input) {
        return CoreValues.bytesHash(
                instructions(reference, input.promptTemplate(), input.experienceConfig())
                        .getBytes(StandardCharsets.UTF_8));
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
