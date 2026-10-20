package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.*;

/** Closed schemas. Only actual delivered evidence may support generated structured artefacts. */
public final class StudentOutputValidator {
    private StudentOutputValidator() {}

    public static JsonNode validate(
            JsonNode raw,
            String task,
            List<Passage> kb,
            Collection<DomainEvidence> domain,
            JsonNode policy) {
        if (raw == null || !raw.isObject()) invalid();
        var output = (ObjectNode) raw.deepCopy();
        String extra =
                switch (task) {
                    case "INSIGHT_REPORT" -> "report";
                    case "PLAN_SUGGESTION" -> "planSuggestion";
                    default -> null;
                };
        JsonNode artifact = extra == null ? null : output.remove(extra);
        var passages = new ArrayList<>(kb);
        for (var d : domain)
            passages.add(
                    new Passage(
                            d.referenceId(),
                            "0",
                            d.versionId(),
                            "domain",
                            d.kind(),
                            "Core",
                            d.text(),
                            "Core projection",
                            CoreValues.tree(Map.of()),
                            null,
                            null,
                            1,
                            "CORE_TOOL"));
        if ("KB_ONLY".equals(policy.path("grounding").asText()))
            for (var cite : output.path("citations"))
                if (kb.stream()
                        .noneMatch(p -> p.referenceId().equals(cite.path("referenceId").asText())))
                    invalid();
        // Domain evidence is authoritative only when delivered by the current authorized tool run.
        var validated = AgentOutputValidator.validate(output, passages, policy, scores(domain));
        if (!"ANSWER".equals(validated.path("outcome").asText())) {
            if (artifact != null) invalid();
            return validated;
        }
        Set<String> inlineRefs = new HashSet<>();
        passages.forEach(p -> inlineRefs.add(p.referenceId()));
        var inline =
                java.util.regex.Pattern.compile(
                                "\\[(K[0-9]+|(?:AR|AT|SC|EN|SE|CO|QR):[a-zA-Z0-9:-]+)\\]")
                        .matcher(validated.path("text").asText());
        while (inline.find()) if (!inlineRefs.contains(inline.group(1))) invalid();
        if (extra == null) return validated;
        if (artifact == null || !artifact.isObject()) invalid();
        Set<String> references = new HashSet<>();
        kb.forEach(p -> references.add(p.referenceId()));
        domain.forEach(d -> references.add(d.referenceId()));
        if (task.equals("INSIGHT_REPORT")) {
            fields(
                    artifact,
                    Set.of(
                            "schemaVersion",
                            "summary",
                            "hypotheses",
                            "strengthsPracticed",
                            "difficulties",
                            "limitations",
                            "nextSteps",
                            "disclaimer",
                            "evidenceRefs"));
            if (!"careersim-insight-v1".equals(artifact.path("schemaVersion").asText())) invalid();
            text(artifact.path("summary"), 1600);
            text(artifact.path("disclaimer"), 600);
            if (!artifact.path("disclaimer")
                    .asText()
                    .equals(
                            "Exploration support only; this report does not certify professional competence or determine a career."))
                invalid();
            for (String key :
                    List.of("strengthsPracticed", "difficulties", "limitations", "nextSteps"))
                texts(artifact.path(key), 8, 400);
            var practiced = new HashSet<String>();
            for (var d : domain)
                if ("PRACTICED_SKILL".equals(d.kind())) {
                    practiced.add(d.data().path("skillCode").asText());
                    practiced.add(d.data().path("skillNameSnapshot").asText());
                }
            for (var skill : artifact.path("strengthsPracticed"))
                if (!practiced.contains(skill.asText())) invalid();
            if (artifact.path("limitations").isEmpty() || artifact.path("nextSteps").isEmpty())
                invalid();
            var hypotheses = artifact.path("hypotheses");
            if (!hypotheses.isArray() || hypotheses.size() > 5) invalid();
            for (var h : hypotheses) {
                fields(h, Set.of("direction", "reason", "evidenceRefs"));
                text(h.path("direction"), 160);
                text(h.path("reason"), 600);
                refs(h.path("evidenceRefs"), references);
                if (h.path("evidenceRefs").isEmpty()) invalid();
                for (var ref : h.path("evidenceRefs")) {
                    boolean included = false;
                    for (var global : artifact.path("evidenceRefs"))
                        if (ref.equals(global)) included = true;
                    if (!included) invalid();
                }
            }
            refs(artifact.path("evidenceRefs"), references);
            if (artifact.path("evidenceRefs").isEmpty()) invalid();
        } else {
            fields(
                    artifact,
                    Set.of("schemaVersion", "title", "steps", "limitations", "evidenceRefs"));
            if (!"careersim-plan-v1".equals(artifact.path("schemaVersion").asText())) invalid();
            text(artifact.path("title"), 120);
            texts(artifact.path("steps"), 5, 240);
            if (artifact.path("steps").isEmpty()) invalid();
            texts(artifact.path("limitations"), 5, 400);
            refs(artifact.path("evidenceRefs"), references);
        }
        // Reuse existing narrative safety checks for every nested text, never trust additional
        // report fields.
        safety(artifact, scores(domain));
        var result = (ObjectNode) validated;
        result.set(extra, artifact);
        if (task.equals("PLAN_SUGGESTION"))
            result.put("planContentHash", CoreValues.hash(artifact));
        return result;
    }

    public static List<Integer> scores(Collection<DomainEvidence> evidence) {
        var scores = new LinkedHashSet<Integer>();
        for (var d : evidence) {
            if ("ASSESSMENT_RESULT".equals(d.kind()))
                for (var dimension : d.data().path("resultData").path("dimensions"))
                    if (dimension.path("raw").isIntegralNumber())
                        scores.add(dimension.path("raw").asInt());
            if ("FINAL_QUIZ_RESULT".equals(d.kind())
                    && d.data().path("quizResult").path("percentage").isIntegralNumber())
                scores.add(d.data().path("quizResult").path("percentage").asInt());
        }
        return List.copyOf(scores);
    }

    private static void safety(JsonNode n, List<Integer> scores) {
        if (n.isTextual())
            edu.hcmute.careersim.guidance.domain.SynReplyValidator.checkEvidence(
                    new SynGuidanceProvider.AiReply(n.asText(), List.of()), scores);
        else if (n.isContainerNode()) n.forEach(child -> safety(child, scores));
    }

    private static void fields(JsonNode n, Set<String> keys) {
        if (!n.isObject() || n.size() != keys.size()) invalid();
        n.fieldNames()
                .forEachRemaining(
                        k -> {
                            if (!keys.contains(k)) invalid();
                        });
    }

    private static void refs(JsonNode n, Set<String> known) {
        if (!n.isArray() || n.size() > 30) invalid();
        var seen = new HashSet<String>();
        for (var x : n)
            if (!x.isTextual() || !known.contains(x.asText()) || !seen.add(x.asText())) invalid();
    }

    private static void texts(JsonNode n, int count, int length) {
        if (!n.isArray() || n.size() > count) invalid();
        for (var x : n) text(x, length);
    }

    private static void text(JsonNode n, int max) {
        if (!n.isTextual()
                || n.asText().isBlank()
                || n.asText().length() > max
                || n.asText()
                        .codePoints()
                        .anyMatch(
                                c ->
                                        Character.isISOControl(c) && c != '\n' && c != '\t'
                                                || c >= 0x202a && c <= 0x202e
                                                || c >= 0x2066 && c <= 0x2069)) invalid();
    }

    private static void invalid() {
        throw new SynGuidanceProvider.ProviderException(
                SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
    }
}
