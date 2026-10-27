package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;

/** Projection of the existing output validators for provider-constrained generation. */
public final class AgentResponseSchema {
    private AgentResponseSchema() {}

    /** Provider wire bounds; exact quote/reference pairing remains host-validated. */
    public static JsonNode grounded(
            String task,
            JsonNode policy,
            Collection<edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage> kb,
            Collection<DomainEvidence> domain) {
        var schema = (com.fasterxml.jackson.databind.node.ObjectNode) forTask(task, policy);
        var options = citationOptions(kb, domain);
        var citations =
                (com.fasterxml.jackson.databind.node.ObjectNode)
                        schema.path("properties").path("citations");
        if (options.isEmpty()) citations.put("maxItems", 0);
        else
            citations.set(
                    "items",
                    CoreValues.tree(
                            object(
                                    Map.of(
                                            "referenceId",
                                                    Map.of(
                                                            "type",
                                                            "string",
                                                            "enum",
                                                            options.stream()
                                                                    .map(p -> p.get("referenceId"))
                                                                    .distinct()
                                                                    .toList()),
                                            "quote",
                                                    text(
                                                            600,
                                                            "Copy one short exact literal quote from currentRunCitationOptionsUntrustedData for the same referenceId. Never paraphrase a quote.")))));
        return schema;
    }

    public static List<Map<String, String>> citationOptions(
            Collection<edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage> kb,
            Collection<DomainEvidence> domain) {
        var delivered = new LinkedHashMap<String, String>();
        kb.forEach(p -> delivered.put(p.referenceId(), p.text()));
        domain.forEach(d -> delivered.put(d.referenceId(), d.text()));
        var choices = new ArrayList<Map<String, String>>();
        delivered.entrySet().stream()
                .limit(20)
                .forEach(
                        e -> {
                            var quotes = new LinkedHashSet<String>();
                            String source = e.getValue();
                            if (!source.isBlank()) {
                                quotes.add(window(source, 0));
                                for (String key :
                                        List.of(
                                                "\"resultData\":",
                                                "\"status\":",
                                                "\"progression\":",
                                                "\"title\":")) {
                                    int at = source.indexOf(key);
                                    if (at >= 0 && quotes.size() < 3)
                                        quotes.add(window(source, at));
                                }
                                quotes.forEach(
                                        quote ->
                                                choices.add(
                                                        Map.of(
                                                                "referenceId",
                                                                e.getKey(),
                                                                "quote",
                                                                quote)));
                            }
                        });
        return List.copyOf(choices);
    }

    private static String window(String text, int start) {
        int count = Math.min(120, text.codePointCount(start, text.length()));
        return text.substring(start, text.offsetByCodePoints(start, count));
    }

    public static JsonNode forTask(String task, JsonNode policy) {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("schemaVersion", Map.of("type", "integer", "enum", List.of(1)));
        properties.put(
                "outcome",
                Map.of(
                        "type",
                        "string",
                        "enum",
                        List.of("ANSWER", "INSUFFICIENT_EVIDENCE", "REFUSED")));
        properties.put(
                "text",
                text(
                        policy.path("maxCharacters").asInt(4000),
                        "User-facing answer; no raw numeric assessment scores."));
        properties.put(
                "citations",
                array(
                        object(
                                Map.of(
                                        "referenceId",
                                        text(
                                                80,
                                                "Exact current-run evidence reference; one citation per unique reference."),
                                        "quote",
                                        text(
                                                600,
                                                "Short verbatim substring of that evidence, never a paraphrase."))),
                        10));
        properties.put(
                "suggestions", array(text(120, "Short next question, at most 120 characters."), 3));
        var required = new ArrayList<>(properties.keySet());
        if ("PLAN_SUGGESTION".equals(task))
            properties.put(
                    "planSuggestion",
                    object(
                            Map.of(
                                    "schemaVersion", enumeration("careersim-plan-v1"),
                                    "title", text(120, "UNSAVED draft title"),
                                    "steps", array(text(240, "One practical step"), 5),
                                    "limitations", array(text(400, "Evidence limitation"), 5),
                                    "evidenceRefs",
                                            array(text(80, "Exact delivered reference"), 30))));
        if ("INSIGHT_REPORT".equals(task)) {
            var report = new LinkedHashMap<String, Object>();
            report.put("schemaVersion", enumeration("careersim-insight-v1"));
            report.put("summary", text(1600, "Evidence-based exploration summary"));
            report.put(
                    "hypotheses",
                    array(
                            object(
                                    Map.of(
                                            "direction",
                                            text(160, "Tentative exploration direction"),
                                            "reason",
                                            text(600, "Reason from authorized current evidence"),
                                            "evidenceRefs",
                                            array(
                                                    text(
                                                            80,
                                                            "Exact delivered reference also included in global evidenceRefs"),
                                                    30))),
                            5));
            report.put(
                    "strengthsPracticed",
                    array(
                            text(
                                    400,
                                    "Only exact skillCode or skillNameSnapshot from current PRACTICED_SKILL evidence; [] if none."),
                            8));
            for (var key : List.of("difficulties", "limitations", "nextSteps"))
                report.put(key, array(text(400, "Short evidence-based item"), 8));
            report.put(
                    "disclaimer",
                    enumeration(
                            "Exploration support only; this report does not certify professional competence or determine a career."));
            report.put("evidenceRefs", array(text(80, "Exact delivered reference"), 30));
            properties.put("report", object(report));
        }
        // Artifact is optional for refusal/insufficient evidence. Host requires it for ANSWER.
        return CoreValues.tree(
                Map.of(
                        "type",
                        "object",
                        "properties",
                        properties,
                        "required",
                        required,
                        "additionalProperties",
                        false));
    }

    private static Object enumeration(String value) {
        return Map.of("type", "string", "enum", List.of(value));
    }

    private static Object text(int max, String description) {
        return Map.of(
                "type", "string", "description", description + " Maximum " + max + " characters.");
    }

    private static Object array(Object item, int max) {
        return Map.of("type", "array", "items", item, "maxItems", max);
    }

    private static Object object(Map<String, Object> fields) {
        return Map.of(
                "type",
                "object",
                "properties",
                fields,
                "required",
                new ArrayList<>(fields.keySet()),
                "additionalProperties",
                false);
    }
}
