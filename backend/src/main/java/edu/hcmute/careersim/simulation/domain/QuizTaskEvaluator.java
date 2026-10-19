package edu.hcmute.careersim.simulation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Deterministic, versioned exact-choice scoring. A score never controls completion. */
@Component
public class QuizTaskEvaluator {
    public void publication(Task task) {
        if (task == null
                || task.definitions() == null
                || task.definitions().stream().anyMatch(Objects::isNull))
            CoreValues.invalid("Task and deliverable definitions required.");
        if (task.taskType() == null
                || !Set.of("QUIZ", "TEXT", "PRACTICAL").contains(task.taskType()))
            CoreValues.invalid("Select an explicit task type.");
        var rubric = task.rubric();
        if (rubric == null) return;
        if (!"QUIZ".equals(task.taskType())
                || rubric.version() < 1
                || !"quiz-exact/1".equals(rubric.evaluatorKey()))
            CoreValues.invalid("Unsupported quiz rubric.");
        var config = rubric.quizScoringConfig();
        CoreValues.object(config, Set.of("schemaVersion", "definitionPosition", "answers"));
        if (!CoreValues.schemaOne(config)
                || !config.path("definitionPosition").isIntegralNumber()
                || !config.path("answers").isObject()
                || config.path("answers").isEmpty()
                || config.path("answers").size() > 100)
            CoreValues.invalid("Invalid quiz scoring configuration.");
        var definition =
                task.definitions().stream()
                        .filter(d -> d.position() == config.path("definitionPosition").asInt())
                        .findFirst()
                        .orElse(null);
        if (definition == null || !definition.required() || !"JSON".equals(definition.format()))
            CoreValues.invalid("Quiz rubric requires a required JSON deliverable.");
        var schema = definition.validationConfig().path("jsonSchema");
        config.path("answers")
                .fields()
                .forEachRemaining(
                        e -> {
                            var property = schema.path("properties").path(e.getKey());
                            boolean required = false, allowed = false;
                            for (var r : schema.path("required"))
                                if (e.getKey().equals(r.asText())) required = true;
                            for (var option : property.path("enum"))
                                if (option.equals(e.getValue())) allowed = true;
                            if (!e.getValue().isTextual() || !required || !allowed)
                                CoreValues.invalid(
                                        "Quiz answer must match a required, enumerated question.");
                        });
    }

    public JsonNode evaluate(Task task, List<Item> items) {
        if (task.rubric() == null) return null;
        var config = task.rubric().quizScoringConfig();
        var definition =
                task.definitions().stream()
                        .filter(d -> d.position() == config.path("definitionPosition").asInt())
                        .findFirst()
                        .orElseThrow();
        var answer =
                items.stream()
                        .filter(i -> i.definitionId().equals(definition.id()))
                        .findFirst()
                        .orElseThrow()
                        .contentJson();
        int correct = 0, total = config.path("answers").size();
        var expected = config.path("answers").fields();
        while (expected.hasNext()) {
            var e = expected.next();
            if (e.getValue().equals(answer.path(e.getKey()))) correct++;
        }
        return CoreValues.tree(
                Map.of(
                        "schemaVersion",
                        1,
                        "correct",
                        correct,
                        "total",
                        total,
                        "percentage",
                        correct * 100 / total,
                        "rubricVersion",
                        task.rubric().version()));
    }
}
