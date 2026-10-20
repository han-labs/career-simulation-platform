package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.guidance.domain.SynReplyValidator;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Passage;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.*;

public final class AgentOutputValidator {
    private AgentOutputValidator() {}

    public static JsonNode validate(JsonNode output, List<Passage> delivered, JsonNode policy) {
        if (output == null
                || !output.isObject()
                || output.size() != 5
                || !Set.of("schemaVersion", "outcome", "text", "citations", "suggestions")
                        .containsAll(keys(output))
                || !output.path("schemaVersion").isIntegralNumber()
                || output.path("schemaVersion").asInt() != 1) invalid();
        String outcome = output.path("outcome").asText();
        if (!Set.of("ANSWER", "INSUFFICIENT_EVIDENCE", "REFUSED").contains(outcome)) invalid();
        text(output.path("text"), policy.path("maxCharacters").asInt(4000));
        var citations = output.path("citations");
        if (!citations.isArray() || citations.size() > 10) invalid();
        var seen = new HashSet<String>();
        for (var citation : citations) {
            if (!citation.isObject()
                    || citation.size() != 2
                    || !citation.has("referenceId")
                    || !citation.has("quote")) invalid();
            text(citation.path("referenceId"), 80);
            text(citation.path("quote"), 600);
            String ref = citation.path("referenceId").asText();
            if (!seen.add(ref)
                    || delivered.stream()
                            .noneMatch(
                                    p ->
                                            p.referenceId().equals(ref)
                                                    && p.text()
                                                            .contains(
                                                                    citation.path("quote")
                                                                            .asText()))) invalid();
        }
        if (!"ANSWER".equals(outcome) && !citations.isEmpty()) invalid();
        if ("ANSWER".equals(outcome)
                && "KB_ONLY".equals(policy.path("grounding").asText())
                && citations.isEmpty())
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.EMPTY_EVIDENCE);
        var suggestions = output.path("suggestions");
        if (!suggestions.isArray() || suggestions.size() > 3) invalid();
        for (var s : suggestions) {
            text(s, 120);
            SynReplyValidator.checkEvidence(
                    new SynGuidanceProvider.AiReply(s.asText(), List.of()), List.of());
        }
        SynReplyValidator.checkEvidence(
                new SynGuidanceProvider.AiReply(output.path("text").asText(), List.of()),
                List.of());
        return output.deepCopy();
    }

    private static Set<String> keys(JsonNode node) {
        var set = new HashSet<String>();
        node.fieldNames().forEachRemaining(set::add);
        return set;
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
