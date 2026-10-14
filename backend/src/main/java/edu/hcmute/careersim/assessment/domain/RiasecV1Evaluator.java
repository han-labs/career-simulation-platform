package edu.hcmute.careersim.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import org.springframework.stereotype.Component;

/** Frozen riasec-sum/1. Change methodology only under a new registered version. */
@Component
public class RiasecV1Evaluator {
    public static final List<String> DIMENSIONS = List.of("R", "I", "A", "S", "E", "C");

    public void publication(VersionDraft v) {
        if (v == null) CoreValues.invalid("Assessment version required.");
        if (!"riasec-sum".equals(v.evaluatorKey()) || !"1".equals(v.evaluatorVersion()))
            CoreValues.invalid("Unsupported evaluator/version.");
        CoreValues.object(
                v.scoringConfig(),
                Set.of(
                        "schemaVersion",
                        "dimensions",
                        "aggregation",
                        "minAnswer",
                        "maxAnswer",
                        "tieOrder"));
        var expected =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                DIMENSIONS,
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                DIMENSIONS));
        if (!expected.equals(v.scoringConfig()))
            CoreValues.invalid("RIASEC scoring configuration must match version 1.");
        Set<Integer> positions = new HashSet<>();
        Set<String> dimensions = new HashSet<>();
        if (v.questions() == null || v.questions().isEmpty() || v.questions().size() > 200)
            CoreValues.invalid("Invalid question set.");
        for (var q : v.questions()) {
            if (q == null) CoreValues.invalid("Assessment question required.");
            CoreValues.text(q.prompt(), 10000);
            if (q.position() < 1 || !positions.add(q.position()))
                CoreValues.invalid("Duplicate/invalid question position.");
            CoreValues.object(
                    q.responseConfig(), Set.of("schemaVersion", "min", "max", "dimension"));
            String d = q.responseConfig().path("dimension").asText();
            if (!q.required()
                    || !"LIKERT".equals(q.responseType())
                    || !CoreValues.schemaOne(q.responseConfig())
                    || !q.responseConfig().path("min").equals(CoreValues.tree(1))
                    || !q.responseConfig().path("max").equals(CoreValues.tree(5))
                    || !DIMENSIONS.contains(d))
                CoreValues.invalid("Unsupported RIASEC question configuration.");
            translations(q.translations());
            dimensions.add(d);
        }
        if (!dimensions.containsAll(DIMENSIONS))
            CoreValues.invalid("All six dimensions are required.");
    }

    private void translations(JsonNode node) {
        CoreValues.object(node, Set.of("en", "vi"));
        node.forEach(
                n -> {
                    CoreValues.object(n, Set.of("prompt"));
                    if (n.has("prompt")) CoreValues.text(n.path("prompt").asText(), 10000);
                });
    }

    public List<Answer> validate(
            List<Question> questions, List<Answer> answers, boolean finalizing) {
        if (answers == null || answers.size() > 200) CoreValues.invalid("Invalid response set.");
        Map<String, Question> all = new HashMap<>();
        questions.forEach(q -> all.put(q.id(), q));
        Set<String> seen = new HashSet<>();
        for (var answer : answers) {
            if (answer == null) CoreValues.invalid("Assessment response required.");
            var q = all.get(answer.questionId());
            if (q == null || !seen.add(answer.questionId()))
                CoreValues.invalid("Unknown, duplicate or wrong-version question.");
            CoreValues.object(answer.answerData(), Set.of("schemaVersion", "value"));
            var n = answer.answerData();
            if (!CoreValues.schemaOne(n)
                    || !n.path("value").isIntegralNumber()
                    || !n.path("value").canConvertToInt()
                    || n.path("value").asInt() < 1
                    || n.path("value").asInt() > 5)
                CoreValues.invalid("Answer must be an integer from 1 to 5.");
        }
        if (finalizing
                && questions.stream()
                        .filter(Question::required)
                        .anyMatch(q -> !seen.contains(q.id())))
            CoreValues.invalid("Answer all required questions.");
        return answers.stream()
                .sorted(Comparator.comparingLong(a -> CoreValues.id(a.questionId())))
                .toList();
    }

    public JsonNode evaluate(
            String type, String version, List<Question> questions, List<Answer> answers) {
        var items = validate(questions, answers, true);
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        DIMENSIONS.forEach(
                d -> {
                    totals.put(d, 0);
                    counts.put(d, 0);
                });
        Map<String, Question> qs = new HashMap<>();
        questions.forEach(q -> qs.put(q.id(), q));
        for (var a : items) {
            String d = qs.get(a.questionId()).responseConfig().path("dimension").asText();
            totals.merge(d, a.answerData().path("value").asInt(), Integer::sum);
            counts.merge(d, 1, Integer::sum);
        }
        var dims =
                DIMENSIONS.stream()
                        .map(
                                d ->
                                        Map.of(
                                                "code",
                                                d,
                                                "raw",
                                                totals.get(d),
                                                "min",
                                                counts.get(d),
                                                "max",
                                                5 * counts.get(d)))
                        .toList();
        var top =
                DIMENSIONS.stream()
                        .sorted(Comparator.comparingInt((String d) -> totals.get(d)).reversed())
                        .limit(3)
                        .toList();
        return CoreValues.tree(
                Map.of(
                        "schemaVersion",
                        1,
                        "assessmentTypeCode",
                        type,
                        "assessmentVersionId",
                        version,
                        "dimensions",
                        dims,
                        "topDimensions",
                        top,
                        "limitationsCode",
                        "EXPLORATION_ONLY"));
    }
}
