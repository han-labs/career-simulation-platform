package edu.hcmute.careersim.guidance.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Validates provider data locally; provider-side schema is not a trust boundary. */
public final class SynReplyValidator {
    private SynReplyValidator() {}

    public static SynGuidanceProvider.AiReply parse(JsonNode payload) {
        if (payload == null
                || !payload.isObject()
                || payload.size() != 2
                || !payload.has("text")
                || !payload.has("suggestions")) invalid();
        payload.fieldNames()
                .forEachRemaining(
                        name -> {
                            if (!Set.of("text", "suggestions").contains(name)) invalid();
                        });
        String text = requiredText(payload.get("text"), 2400);
        JsonNode array = payload.get("suggestions");
        if (!array.isArray() || array.size() > 3) invalid();
        List<String> suggestions = new ArrayList<>();
        for (JsonNode value : array) suggestions.add(requiredText(value, 120));
        if (suggestions.stream().distinct().count() != suggestions.size()) invalid();
        return new SynGuidanceProvider.AiReply(text, List.copyOf(suggestions));
    }

    public static void checkEvidence(
            SynGuidanceProvider.AiReply reply, java.util.List<Integer> scores) {
        String normalized = KnowledgeChunker.normalize(reply.text());
        if (normalized.contains("guaranteed employment")
                || normalized.contains("you must choose")
                || normalized.contains("ban chac chan phu hop")
                || normalized.contains("ban phai chon nghe")) invalid("CAREER_CERTAINTY");
        checkCompetenceClaims(normalized);
        var matcher =
                java.util.regex.Pattern.compile(
                                "(?i)(?:score(?: is| of|:)?|diem(?: cua ban)?(?: la|:)?|điểm(?: của bạn)?(?: là|:)?)\\s*(\\d{1,3})|(?<!\\d)(\\d{1,3})\\s*/\\s*100")
                        .matcher(reply.text());
        while (matcher.find()) {
            int value =
                    Integer.parseInt(
                            matcher.group(1) == null ? matcher.group(2) : matcher.group(1));
            if (!scores.contains(value)) invalid("SCORE_MISMATCH");
        }
        if (reply.text().matches("(?s).*<[^>]+>.*") || reply.text().matches("(?is).*https?://.*"))
            invalid("MARKUP_OR_URL");
    }

    private static void checkCompetenceClaims(String text) {
        var claims =
                java.util.regex.Pattern.compile(
                                "(?:ban|you) (?:co nang luc|rat gioi|gioi|are competent|are talented|are a strong|are best suited|have strong ability|have strong skills|co the manh)|nang luc (?:rat |vo cung )?tot|nghe phu hop nhat voi ban")
                        .matcher(text);
        while (claims.find()) {
            String prefix = text.substring(Math.max(0, claims.start() - 100), claims.start());
            // Permit explicit rejection of a conclusion; do not carry negation across a new clause.
            String[] clauses = prefix.split("[.!?;\\n]|\\b(?:but|however|nhung|tuy nhien)\\b");
            String clause = clauses.length == 0 ? "" : clauses[clauses.length - 1];
            if (!clause.matches(
                    "(?s).*(?:(?:khong|chua) (?:the |du (?:co so|bang chung) de )?(?:ket luan|khang dinh|co nghia)(?: rang| la)?|(?:cannot|can't|does not|doesn't) (?:conclude|claim|mean|establish|show)(?: that)?)\\s*$"))
                invalid("BROAD_COMPETENCE");
        }
    }

    private static String requiredText(JsonNode value, int maximum) {
        if (value == null || !value.isTextual()) {
            invalid();
        }
        String text = value.textValue().trim();
        if (text.length() > maximum) invalid(maximum == 120 ? "SUGGESTION_LENGTH" : "TEXT_LENGTH");
        if (text.isEmpty()
                || text.length() > maximum
                || text.codePoints()
                        .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')
                || text.codePoints()
                        .anyMatch(c -> c >= 0x202a && c <= 0x202e || c >= 0x2066 && c <= 0x2069))
            invalid();
        return text;
    }

    private static void invalid() {
        invalid("SCHEMA");
    }

    private static void invalid(String code) {
        org.slf4j.LoggerFactory.getLogger(SynReplyValidator.class)
                .warn("Syn output rejected check={}", code);
        throw new SynGuidanceProvider.ProviderException(
                SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT,
                new IllegalArgumentException(code));
    }
}
