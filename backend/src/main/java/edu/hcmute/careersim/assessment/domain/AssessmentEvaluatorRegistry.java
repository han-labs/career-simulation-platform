package edu.hcmute.careersim.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Registered deterministic methodologies; every result retains its own type/version. */
@Component
@RequiredArgsConstructor
public class AssessmentEvaluatorRegistry {
    private final RiasecV1Evaluator riasec;

    public void publication(VersionDraft draft) {
        if (draft == null) CoreValues.invalid("Assessment version required.");
        if ("riasec-sum".equals(draft.evaluatorKey())) {
            riasec.publication(draft);
            return;
        }
        if (!"profile-sum".equals(draft.evaluatorKey()) || !"1".equals(draft.evaluatorVersion()))
            CoreValues.invalid("Unsupported evaluator/version.");
        var c = draft.scoringConfig();
        CoreValues.object(
                c,
                Set.of(
                        "schemaVersion",
                        "dimensions",
                        "aggregation",
                        "minAnswer",
                        "maxAnswer",
                        "tieOrder"));
        if (!CoreValues.schemaOne(c)
                || !c.path("dimensions").isArray()
                || c.path("dimensions").isEmpty()
                || c.path("dimensions").size() > 20
                || !c.path("tieOrder").equals(c.path("dimensions"))
                || !"SUM".equals(c.path("aggregation").asText())
                || !c.path("minAnswer").equals(CoreValues.tree(1))
                || !c.path("maxAnswer").equals(CoreValues.tree(5)))
            CoreValues.invalid("Invalid profile-sum/1 configuration.");
        var dimensions = new HashSet<String>();
        for (var dimension : c.path("dimensions"))
            if (!dimension.isTextual()
                    || !dimension.asText().matches("[A-Z][A-Z0-9_]{0,39}")
                    || !dimensions.add(dimension.asText()))
                CoreValues.invalid("Invalid/duplicate profile category.");
        if (draft.questions() == null
                || draft.questions().isEmpty()
                || draft.questions().size() > 200) CoreValues.invalid("Invalid question set.");
        var positions = new HashSet<Integer>();
        var covered = new HashSet<String>();
        for (var q : draft.questions()) {
            if (q == null) CoreValues.invalid("Question required.");
            CoreValues.text(q.prompt(), 10000);
            CoreValues.object(
                    q.responseConfig(), Set.of("schemaVersion", "min", "max", "dimension"));
            String dimension = q.responseConfig().path("dimension").asText();
            if (q.position() < 1
                    || !positions.add(q.position())
                    || !q.required()
                    || !"LIKERT".equals(q.responseType())
                    || !CoreValues.schemaOne(q.responseConfig())
                    || !q.responseConfig().path("min").equals(CoreValues.tree(1))
                    || !q.responseConfig().path("max").equals(CoreValues.tree(5))
                    || !dimensions.contains(dimension))
                CoreValues.invalid("Invalid profile question configuration.");
            CoreValues.object(q.translations(), Set.of("en", "vi"));
            q.translations()
                    .forEach(
                            v -> {
                                CoreValues.object(v, Set.of("prompt"));
                                if (v.has("prompt"))
                                    CoreValues.text(v.path("prompt").asText(), 10000);
                            });
            covered.add(dimension);
        }
        if (!covered.equals(dimensions))
            CoreValues.invalid("Every category requires reviewed questions.");
    }

    public List<Answer> validate(
            List<Question> questions, List<Answer> answers, boolean finalizing) {
        // Both registered methods use the same bounded Likert response contract.
        return riasec.validate(questions, answers, finalizing);
    }

    public JsonNode evaluate(Version v, String type, List<Answer> answers) {
        if ("riasec-sum".equals(v.evaluatorKey()))
            return riasec.evaluate(type, v.id(), v.questions(), answers);
        var ordered = new ArrayList<String>();
        v.scoringConfig().path("dimensions").forEach(d -> ordered.add(d.asText()));
        var totals = new LinkedHashMap<String, Integer>();
        var counts = new HashMap<String, Integer>();
        ordered.forEach(
                d -> {
                    totals.put(d, 0);
                    counts.put(d, 0);
                });
        var questions = new HashMap<String, Question>();
        v.questions().forEach(q -> questions.put(q.id(), q));
        for (var a : validate(v.questions(), answers, true)) {
            String d = questions.get(a.questionId()).responseConfig().path("dimension").asText();
            totals.merge(d, a.answerData().path("value").asInt(), Integer::sum);
            counts.merge(d, 1, Integer::sum);
        }
        var categories =
                ordered.stream()
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
                                                counts.get(d) * 5))
                        .toList();
        int highest = totals.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return CoreValues.tree(
                Map.of(
                        "schemaVersion",
                        1,
                        "assessmentTypeCode",
                        type,
                        "assessmentVersionId",
                        v.id(),
                        "categories",
                        categories,
                        "topCategories",
                        ordered.stream().filter(d -> totals.get(d) == highest).toList(),
                        "interpretationVersion",
                        "profile-sum/1",
                        "limitationsCode",
                        "SELF_REPORTED_EXPLORATION_ONLY"));
    }
}
