package edu.hcmute.careersim.assessment.domain;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VersionedAssessmentEvaluatorTest {
    final RiasecV1Evaluator evaluator = new RiasecV1Evaluator();

    List<Question> questions() {
        var result = new ArrayList<Question>();
        for (var d : RiasecV1Evaluator.DIMENSIONS)
            result.add(
                    new Question(
                            Integer.toString(result.size() + 1),
                            result.size() + 1,
                            "Synthetic " + d,
                            "LIKERT",
                            CoreValues.tree(
                                    Map.of("schemaVersion", 1, "min", 1, "max", 5, "dimension", d)),
                            CoreValues.tree(Map.of()),
                            true));
        return result;
    }

    List<Answer> answers(int value) {
        return questions().stream()
                .map(
                        q ->
                                new Answer(
                                        q.id(),
                                        CoreValues.tree(
                                                Map.of("schemaVersion", 1, "value", value))))
                .toList();
    }

    @Test
    void scoreIsDeterministicWithVersionSpecificRangesAndStableTies() {
        var answers = new ArrayList<>(answers(5));
        var expected = evaluator.evaluate("RIASEC", "11", questions(), answers);
        Collections.reverse(answers);
        assertThat(evaluator.evaluate("RIASEC", "11", questions(), answers)).isEqualTo(expected);
        assertThat(expected.path("topDimensions"))
                .isEqualTo(CoreValues.tree(List.of("R", "I", "A")));
        expected.path("dimensions")
                .forEach(
                        d -> {
                            assertThat(d.path("min").asInt()).isEqualTo(1);
                            assertThat(d.path("max").asInt()).isEqualTo(5);
                            assertThat(d.path("raw").asInt()).isEqualTo(5);
                        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "null",
                "{}",
                "{\"schemaVersion\":1,\"value\":0}",
                "{\"schemaVersion\":1,\"value\":6}",
                "{\"schemaVersion\":1,\"value\":1.5}",
                "{\"schemaVersion\":1,\"value\":\"5\"}",
                "{\"schemaVersion\":2,\"value\":5}",
                "{\"schemaVersion\":1,\"value\":5,\"extra\":true}"
            })
    void malformedAnswersCannotBeSavedOrScored(String json) {
        var invalid = List.of(new Answer("1", CoreValues.json(json)));
        assertThatThrownBy(() -> evaluator.validate(questions(), invalid, false))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> evaluator.validate(questions(), invalid, true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void draftsAllowMissingAnswersButFinalDoesNotAndForeignQuestionsAreRejected() {
        assertThat(evaluator.validate(questions(), List.of(), false)).isEmpty();
        assertThatThrownBy(() -> evaluator.validate(questions(), List.of(), true))
                .hasMessage(
                        "Please answer all required questions before submitting your career quiz.");
        assertThatThrownBy(
                        () ->
                                evaluator.validate(
                                        questions(),
                                        List.of(answers(3).get(0), answers(3).get(0)),
                                        false))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                evaluator.validate(
                                        questions(),
                                        List.of(new Answer("99", answers(3).get(0).answerData())),
                                        false))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void publicationRejectsMissingDimensionsAndUnsupportedMethodology() {
        var config =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "dimensions",
                                RiasecV1Evaluator.DIMENSIONS,
                                "aggregation",
                                "SUM",
                                "minAnswer",
                                1,
                                "maxAnswer",
                                5,
                                "tieOrder",
                                RiasecV1Evaluator.DIMENSIONS));
        var qs =
                questions().stream()
                        .map(
                                q ->
                                        new QuestionDraft(
                                                q.position(),
                                                q.prompt(),
                                                q.responseType(),
                                                q.responseConfig(),
                                                q.translations(),
                                                true))
                        .toList();
        evaluator.publication(new VersionDraft("riasec-sum", "1", config, qs));
        assertThatThrownBy(
                        () ->
                                evaluator.publication(
                                        new VersionDraft("riasec-sum", "2", config, qs)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                evaluator.publication(
                                        new VersionDraft(
                                                "riasec-sum", "1", config, qs.subList(0, 5))))
                .isInstanceOf(ApiException.class);
    }
}
