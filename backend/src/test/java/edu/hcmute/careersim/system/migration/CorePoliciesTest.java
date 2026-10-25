package edu.hcmute.careersim.system.migration;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.assessment.domain.RiasecV1Evaluator;
import edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.*;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;
import org.junit.jupiter.api.Test;

class CorePoliciesTest {
    @Test
    void filePoliciesAcceptOnlySupportedCanonicalMimeTypesWithinTenMib() {
        var validator =
                new edu.hcmute.careersim.simulation.domain.DeliverableValidator(
                        org.mockito.Mockito.mock(
                                edu.hcmute.careersim.common.storage.FileAssetService.class));
        var supported =
                List.of(
                        "text/plain",
                        "application/pdf",
                        "image/png",
                        "image/jpeg",
                        "application/zip",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        for (String mime : supported)
            assertThatCode(
                            () ->
                                    validator.definition(
                                            fileDefinition(List.of(mime), 10L * 1024 * 1024)))
                    .doesNotThrowAnyException();

        assertThatThrownBy(
                        () ->
                                validator.definition(
                                        fileDefinition(
                                                List.of("application/octet-stream"),
                                                10L * 1024 * 1024)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                validator.definition(
                                        fileDefinition(
                                                List.of("application/zip"), 10L * 1024 * 1024 + 1)))
                .isInstanceOf(ApiException.class);
    }

    private static edu.hcmute.careersim.simulation.dto.ScenarioContracts.Definition fileDefinition(
            List<String> mimeTypes, long maxBytes) {
        return new edu.hcmute.careersim.simulation.dto.ScenarioContracts.Definition(
                null,
                1,
                "Task file",
                "FILE",
                true,
                CoreValues.tree(
                        Map.of(
                                "schemaVersion", 1,
                                "maxBytes", maxBytes,
                                "allowedMimeTypes", mimeTypes)));
    }

    @Test
    void textDeliverableWithoutOptionalBoundsAcceptsNonBlankWork() {
        var validator =
                new edu.hcmute.careersim.simulation.domain.DeliverableValidator(
                        org.mockito.Mockito.mock(
                                edu.hcmute.careersim.common.storage.FileAssetService.class));
        var definition =
                new edu.hcmute.careersim.simulation.dto.ScenarioContracts.Definition(
                        "9", 1, "Task response", "TEXT", true, CoreValues.tree(Map.of()));
        var item =
                new edu.hcmute.careersim.simulation.dto.ScenarioContracts.Item(
                        "9", "A specific response", null, null, null);

        assertThat(validator.validate(7, List.of(definition), List.of(item), true))
                .containsExactly(item);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        7,
                                        List.of(definition),
                                        List.of(
                                                new edu.hcmute.careersim.simulation.dto
                                                        .ScenarioContracts.Item(
                                                        "9", "   ", null, null, null)),
                                        true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deliverableSchemaRejectsRemoteRefUnboundedItemsAndOpenProperties() {
        var validator =
                new edu.hcmute.careersim.simulation.domain.DeliverableValidator(
                        org.mockito.Mockito.mock(
                                edu.hcmute.careersim.common.storage.FileAssetService.class));
        for (var schema :
                List.of(
                        Map.of("type", "object", "$ref", "https://example.test/schema"),
                        Map.of(
                                "type",
                                "object",
                                "properties",
                                Map.of("items", Map.of("type", "array"))),
                        Map.of("type", "object", "additionalProperties", true))) {
            var definition =
                    new edu.hcmute.careersim.simulation.dto.ScenarioContracts.Definition(
                            null,
                            1,
                            "Data",
                            "JSON",
                            true,
                            CoreValues.tree(Map.of("schemaVersion", 1, "jsonSchema", schema)));
            assertThatThrownBy(() -> validator.definition(definition))
                    .isInstanceOf(ApiException.class);
        }
    }

    final RiasecV1Evaluator evaluator = new RiasecV1Evaluator();

    List<Question> questions() {
        var rows = new ArrayList<Question>();
        int id = 0;
        for (var d : RiasecV1Evaluator.DIMENSIONS)
            rows.add(
                    new Question(
                            Integer.toString(++id),
                            id,
                            "Fixture",
                            "LIKERT",
                            CoreValues.tree(
                                    Map.of("schemaVersion", 1, "min", 1, "max", 5, "dimension", d)),
                            CoreValues.tree(Map.of()),
                            true));
        return rows;
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
    void pinnedQuestionCountDeterminesExactMinMaxAndStableTieOrder() {
        for (int value : List.of(1, 5)) {
            var result = evaluator.evaluate("RIASEC", "9", questions(), answers(value));
            assertThat(result.path("topDimensions"))
                    .isEqualTo(CoreValues.tree(List.of("R", "I", "A")));
            result.path("dimensions")
                    .forEach(
                            d -> {
                                assertThat(d.path("raw").asInt()).isEqualTo(value);
                                assertThat(d.path("min").asInt()).isEqualTo(1);
                                assertThat(d.path("max").asInt()).isEqualTo(5);
                            });
        }
    }

    @Test
    void evaluatorRejectsOutOfRangeFractionalUnknownAndNullInputs() {
        for (Object value : List.of(0, 6, 1.5, "3")) {
            var rows = new ArrayList<>(answers(3));
            rows.set(
                    0,
                    new Answer("1", CoreValues.tree(Map.of("schemaVersion", 1, "value", value))));
            assertThatThrownBy(() -> evaluator.validate(questions(), rows, true))
                    .isInstanceOf(ApiException.class);
        }
        var wrong = new ArrayList<>(answers(3));
        wrong.set(0, new Answer("999", CoreValues.tree(Map.of("schemaVersion", 1, "value", 3))));
        assertThatThrownBy(() -> evaluator.validate(questions(), wrong, true))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () -> evaluator.validate(questions(), Arrays.asList((Answer) null), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void canonicalHashPreservesUnicodeAndMeaningfulWhitespaceButNormalizesLfAndObjectOrder() {
        var first = new LinkedHashMap<String, Object>();
        first.put("b", "Tiếng Việt\r\nwork");
        first.put("a", List.of(1, 2));
        var second = new LinkedHashMap<String, Object>();
        second.put("a", List.of(1, 2));
        second.put("b", "Tiếng Việt\nwork");
        assertThat(CoreValues.hash(first)).isEqualTo(CoreValues.hash(second));
        assertThat(CoreValues.hash("work ")).isNotEqualTo(CoreValues.hash("work"));
    }

    @Test
    void identityAndRevisionRepresentationsRejectAmbiguity() {
        for (String id : List.of("0", "-1", "01", "1.0", "9223372036854775808"))
            assertThatThrownBy(() -> CoreValues.id(id)).isInstanceOf(ApiException.class);
        assertThat(CoreValues.revision("\"42\"")).isEqualTo(42);
        for (String revision : List.of("W/\"1\"", "\"1", "1\"", "-1", "1,2"))
            assertThatThrownBy(() -> CoreValues.revision(revision))
                    .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> CoreValues.revision(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void unsupportedEvaluatorAndIncompleteDimensionCoverageCannotPublish() {
        var q = questions();
        var draftQuestions =
                q.stream()
                        .map(
                                x ->
                                        new QuestionDraft(
                                                x.position(),
                                                x.prompt(),
                                                x.responseType(),
                                                x.responseConfig(),
                                                x.translations(),
                                                true))
                        .toList();
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
        assertThatThrownBy(
                        () ->
                                evaluator.publication(
                                        new VersionDraft("unknown", "1", config, draftQuestions)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                evaluator.publication(
                                        new VersionDraft(
                                                "riasec-sum",
                                                "1",
                                                config,
                                                draftQuestions.subList(0, 5))))
                .isInstanceOf(ApiException.class);
    }
}
