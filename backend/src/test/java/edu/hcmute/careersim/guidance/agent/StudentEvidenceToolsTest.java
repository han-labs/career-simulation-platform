package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.assessment.access.VersionedAssessmentAccess;
import edu.hcmute.careersim.assessment.service.VersionedAssessmentService;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.domain.*;
import edu.hcmute.careersim.guidance.agent.service.*;
import edu.hcmute.careersim.simulation.access.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StudentEvidenceToolsTest {
    final VersionedAssessmentAccess assessments = mock(VersionedAssessmentAccess.class);
    final VersionedAssessmentService catalogue = mock(VersionedAssessmentService.class);
    final ScenarioCatalogAccess scenarios = mock(ScenarioCatalogAccess.class);
    final ParticipationEvidenceAccess evidence = mock(ParticipationEvidenceAccess.class);
    final EnrollmentService enrollments = mock(EnrollmentService.class);
    final TaskSubmissionService submissions = mock(TaskSubmissionService.class);
    final StudentEvidenceTools tools =
            new StudentEvidenceTools(
                    assessments, catalogue, scenarios, evidence, enrollments, submissions);

    @Test
    void privateToolHasNoIdentitySelectorAndPreviewCannotImpersonate() {
        assertThatThrownBy(
                        () ->
                                tools.invoke(
                                        7,
                                        "simulation.myEvidence",
                                        CoreValues.tree(Map.of("studentId", 8))))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        assertThatThrownBy(() -> tools.preview("simulation.myEvidence", CoreValues.tree(Map.of())))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class);
        verifyNoInteractions(evidence, enrollments, submissions);
    }

    @Test
    void quizZeroIsRealEvidenceAndNoAnswerKeyOrDraftPayloadEscapes() {
        var metadata =
                new Metadata(
                        "Synthetic scenario",
                        "Developer",
                        "BACKEND",
                        "Practice",
                        null,
                        null,
                        "INTRODUCTORY",
                        10,
                        null,
                        null);
        when(evidence.readOwned(7, 20)).thenReturn(List.of());
        when(evidence.completions(7)).thenReturn(List.of());
        when(enrollments.list(7, 0, 5))
                .thenReturn(
                        List.of(
                                new Enrollment(
                                        "11",
                                        "12",
                                        "IN_PROGRESS",
                                        0,
                                        null,
                                        null,
                                        null,
                                        null,
                                        metadata,
                                        List.of(),
                                        1,
                                        1,
                                        "ORDERED_SELF_REVIEW",
                                        List.of(new TaskProgress("13", true, true, false)))));
        var score =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "correct",
                                0,
                                "total",
                                1,
                                "percentage",
                                0,
                                "rubricVersion",
                                1));
        when(submissions.read(7, 11, 13))
                .thenReturn(
                        new Submission(
                                "14",
                                "11",
                                "13",
                                "12",
                                "FINAL",
                                0,
                                null,
                                null,
                                List.of(new Item("15", "private raw submission", null, null, null)),
                                score,
                                null,
                                "7"));
        var result = tools.invoke(7, "simulation.myEvidence", CoreValues.tree(Map.of()));
        assertThat(CoreValues.write(result))
                .contains("FINAL_QUIZ_RESULT", "\"percentage\":0", "OWN_ENROLLMENT_PROGRESS")
                .doesNotContain("private raw submission", "quizScoringConfig", "modelAnswer");
        verify(submissions).read(7, 11, 13);
        var quiz =
                result.stream()
                        .filter(x -> x.kind().equals("FINAL_QUIZ_RESULT"))
                        .findFirst()
                        .orElseThrow();
        var output =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                "ANSWER",
                                "text",
                                "Your quiz score is 0.",
                                "citations",
                                List.of(
                                        Map.of(
                                                "referenceId",
                                                quiz.referenceId(),
                                                "quote",
                                                quiz.text())),
                                "suggestions",
                                List.of()));
        assertThat(
                        StudentOutputValidator.validate(
                                        output,
                                        "CHAT",
                                        List.of(),
                                        result,
                                        CoreValues.tree(
                                                Map.of(
                                                        "grounding",
                                                        "GENERAL_ALLOWED",
                                                        "maxCharacters",
                                                        4000)))
                                .path("text")
                                .asText())
                .contains("0");
        assertThatThrownBy(
                        () ->
                                StudentOutputValidator.validate(
                                        output,
                                        "CHAT",
                                        List.of(),
                                        List.of(),
                                        CoreValues.tree(
                                                Map.of(
                                                        "grounding",
                                                        "GENERAL_ALLOWED",
                                                        "maxCharacters",
                                                        4000))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void toolArgumentsAreClosedBoundedAndCredentialsMinimized() {
        var args =
                StudentEvidenceTools.arguments(
                        "kb.search",
                        CoreValues.tree(
                                Map.of("query", "api_key=synthetic-secret-value", "topK", 2)));
        assertThat(args.path("query").asText()).isEqualTo("[redacted]");
        assertThat(args.path("topK").asInt()).isEqualTo(2);
        assertThatThrownBy(
                        () ->
                                StudentEvidenceTools.arguments(
                                        "kb.search",
                                        CoreValues.tree(Map.of("query", "practice", "topK", 11))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                StudentEvidenceTools.arguments(
                                        "kb.search",
                                        CoreValues.tree(
                                                Map.of(
                                                        "query",
                                                        "practice",
                                                        "knowledgeBaseId",
                                                        "foreign"))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void canonicalToolSchemasAndBackendValidationRemainClosedForEveryTool() {
        for (var definition : StudentEvidenceTools.definitions(StudentEvidenceTools.NAMES)) {
            assertThat(definition.parameters().path("additionalProperties").isBoolean()).isTrue();
            assertThat(definition.parameters().path("additionalProperties").asBoolean()).isFalse();
            var args = CoreValues.tree(Map.of("query", "practice", "unexpected", "not permitted"));
            assertThatThrownBy(() -> StudentEvidenceTools.arguments(definition.name(), args))
                    .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class)
                    .hasMessage("Unknown tool argument.");
        }
        assertThatThrownBy(
                        () ->
                                tools.preview(
                                        "simulation.explore",
                                        CoreValues.tree(Map.of("unexpected", "not permitted"))))
                .isInstanceOf(edu.hcmute.careersim.common.exception.ApiException.class)
                .hasMessage("Unknown tool argument.");
        verifyNoInteractions(assessments, catalogue, scenarios, evidence, enrollments, submissions);
    }

    @Test
    void finalizedEvidenceAuthorityIsRecheckedBeforeReuse() {
        var source =
                new DomainEvidence(
                        "SE:14:0",
                        "PRACTICED_SKILL",
                        "OWNER",
                        "12",
                        "API practice",
                        CoreValues.tree(Map.of("submissionId", "14", "skillCode", "API")));
        when(evidence.ownsFinal(7, "14", "12", "API")).thenReturn(true, false);
        assertThat(tools.available(7, List.of(source))).isTrue();
        assertThat(tools.available(7, List.of(source))).isFalse();
        verify(evidence, times(2)).ownsFinal(7, "14", "12", "API");
    }
}
