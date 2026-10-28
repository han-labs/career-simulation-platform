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
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void ownerActivity() {
        when(assessments.activity(eq(7L), any()))
                .thenAnswer(
                        call ->
                                new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                        .OwnActivity(call.getArgument(1), 0, 0, 0, 0, 0));
        when(enrollments.activity(7)).thenReturn(new OwnActivity(0, 0, 0, 0, 0));
    }

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

    @Test
    void publicDiscoveryRequiresNoEnrollmentAndExposesUsefulDetailsWithoutAnswers() {
        var metadata =
                new Metadata(
                        "Backend practice",
                        "Backend developer",
                        "BACKEND",
                        "Explore APIs",
                        "Public introduction",
                        "Practice request handling",
                        "INTRODUCTORY",
                        25,
                        null,
                        null);
        when(scenarios.search(any()))
                .thenReturn(
                        new CatalogPage(
                                List.of(
                                        new Summary(
                                                "10",
                                                "backend-practice",
                                                "12",
                                                "2",
                                                "Example",
                                                metadata)),
                                null));
        var result = tools.invoke(7, "simulation.explore", CoreValues.tree(Map.of()));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).access()).isEqualTo("PUBLIC");
        assertThat(result.get(0).data().path("estimatedMinutes").asInt()).isEqualTo(25);
        assertThat(CoreValues.write(result))
                .contains("Backend developer", "Practice request handling", "Public introduction")
                .doesNotContain("modelAnswer", "rubric", "quizScoringConfig", "password");
        verify(scenarios).search(argThat(q -> q.q().isEmpty() && q.limit() == 8));
        verifyNoInteractions(assessments, catalogue, evidence, enrollments, submissions);
    }

    @Test
    void newStudentCanDiscoverPublishedAssessmentsWithoutAnyPersonalResult() {
        when(catalogue.publicCatalog())
                .thenReturn(
                        List.of(
                                new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                        .PublicType(
                                        "1",
                                        "RIASEC",
                                        "Career interests",
                                        "2",
                                        42,
                                        edu.hcmute.careersim.assessment.domain.AssessmentCatalogInfo
                                                .describe("RIASEC", 42))));
        when(assessments.readOwned(7, null)).thenReturn(List.of());
        var result =
                tools.invoke(7, "assessment.lookup", CoreValues.tree(Map.of("scope", "CATALOG")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo("ASSESSMENT_CATALOG");
        assertThat(result.get(0).access()).isEqualTo("PUBLIC");
        assertThat(CoreValues.write(result))
                .doesNotContain("answers", "questions", "scoringConfig");
        verifyNoInteractions(evidence, enrollments, submissions);
    }

    @Test
    void publicEvidenceCanGroundAFirstExplorationDraftWithoutPersonalAttempts() {
        when(catalogue.publicCatalog())
                .thenReturn(
                        List.of(
                                new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                        .PublicType(
                                        "1",
                                        "RIASEC",
                                        "Career interests",
                                        "2",
                                        42,
                                        edu.hcmute.careersim.assessment.domain.AssessmentCatalogInfo
                                                .describe("RIASEC", 42))));
        when(assessments.readOwned(7, null)).thenReturn(List.of());
        var evidence =
                tools.invoke(7, "assessment.lookup", CoreValues.tree(Map.of("scope", "CATALOG")));
        var draft =
                CoreValues.tree(
                        Map.of(
                                "schemaVersion",
                                1,
                                "outcome",
                                "ANSWER",
                                "text",
                                "An unsaved starting plan using public choices.",
                                "citations",
                                List.of(),
                                "suggestions",
                                List.of(),
                                "planSuggestion",
                                Map.of(
                                        "schemaVersion",
                                        "careersim-plan-v1",
                                        "title",
                                        "Explore interests",
                                        "steps",
                                        List.of("Try the career interests assessment."),
                                        "limitations",
                                        List.of("No personal results yet."),
                                        "evidenceRefs",
                                        List.of("AT:1"))));
        var policy = CoreValues.tree(Map.of("grounding", "GENERAL_ALLOWED", "maxCharacters", 4000));
        assertThat(
                        StudentOutputValidator.validate(
                                        draft, "PLAN_SUGGESTION", List.of(), evidence, policy)
                                .has("planContentHash"))
                .isTrue();
        assertThatThrownBy(
                        () ->
                                StudentOutputValidator.validate(
                                        draft, "PLAN_SUGGESTION", List.of(), List.of(), policy))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void advertisedSimulationQueryBoundsMatchTheExistingCatalogueService() {
        assertThat(
                        StudentEvidenceTools.arguments(
                                "simulation.explore",
                                CoreValues.tree(
                                        Map.of("query", "", "careerTrack", "", "difficulty", ""))))
                .isEqualTo(CoreValues.tree(Map.of("query", "")));
        assertThatThrownBy(
                        () ->
                                StudentEvidenceTools.arguments(
                                        "kb.search", CoreValues.tree(Map.of("query", ""))))
                .isInstanceOf(RuntimeException.class);
        var tool = StudentEvidenceTools.definitions(List.of("simulation.explore")).get(0);
        assertThat(tool.parameters().path("properties").path("query").path("maxLength").asInt())
                .isEqualTo(200);
        assertThat(
                        tool.parameters()
                                .path("properties")
                                .path("careerTrack")
                                .path("maxLength")
                                .asInt())
                .isEqualTo(60);
        assertThatThrownBy(
                        () ->
                                StudentEvidenceTools.arguments(
                                        "simulation.explore",
                                        CoreValues.tree(Map.of("query", "x".repeat(201)))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void allFourPublicQuizDescriptionsAndLiveCountsFitTheExistingObservationBudget() {
        var codes = List.of("RIASEC", "INTERESTS", "ABILITIES", "WORK_ACTIVITIES");
        var counts = List.of(42, 12, 12, 9);
        var descriptions =
                new ArrayList<
                        edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .PublicType>();
        for (int i = 0; i < codes.size(); i++)
            descriptions.add(
                    new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.PublicType(
                            Integer.toString(i + 1),
                            codes.get(i),
                            codes.get(i),
                            Integer.toString(i + 1),
                            counts.get(i),
                            edu.hcmute.careersim.assessment.domain.AssessmentCatalogInfo.describe(
                                    codes.get(i), counts.get(i))));
        when(catalogue.publicCatalog()).thenReturn(descriptions);
        when(assessments.readOwned(7, null)).thenReturn(List.of());
        var result =
                tools.invoke(7, "assessment.lookup", CoreValues.tree(Map.of("scope", "CATALOG")));
        assertThat(result).hasSize(4);
        assertThat(result)
                .allSatisfy(
                        d -> {
                            assertThat(d.access()).isEqualTo("PUBLIC");
                            assertThat(d.data().path("overview").has("whenToUse")).isTrue();
                        });
        assertThat(result.stream().map(d -> d.data().path("questionCount").asInt()).toList())
                .containsExactly(42, 12, 12, 9);
        assertThat(CoreValues.write(result))
                .doesNotContain("scoringConfig", "responseConfig", "answerData");
        verifyNoInteractions(evidence, enrollments, submissions);
    }

    @Test
    void personalScopePrioritisesRealOwnResultsAndTotalsWithoutPublicCatalogue() {
        when(assessments.activity(7, "ABILITIES"))
                .thenReturn(
                        new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .OwnActivity("ABILITIES", 23, 1, 22, 22, 1));
        var row =
                new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts.Result(
                        "8",
                        "ABILITIES",
                        "2",
                        "profile-sum",
                        "1",
                        CoreValues.tree(
                                Map.of(
                                        "categories",
                                        List.of(Map.of("code", "TECHNICAL_LEARNING", "raw", 13)))),
                        java.time.Instant.EPOCH);
        when(assessments.readOwned(7, "ABILITIES")).thenReturn(List.of(row));
        var result =
                tools.invoke(
                        7,
                        "assessment.lookup",
                        CoreValues.tree(
                                Map.of("scope", "PERSONAL", "assessmentType", "ABILITIES")));
        assertThat(result)
                .extracting(DomainEvidence::kind)
                .containsExactly("OWN_ASSESSMENT_ACTIVITY", "ASSESSMENT_RESULT");
        assertThat(result.get(0).data().path("savedResults").asInt()).isEqualTo(22);
        assertThat(result.get(1).data().path("attemptId").asText()).isEqualTo("8");
        assertThat(StudentOutputValidator.scores(result)).contains(13);
        verifyNoInteractions(catalogue);
    }

    @Test
    void publicScopeAndAdminPreviewNeverReadStudentActivityOrResults() {
        when(catalogue.publicCatalog()).thenReturn(List.of());
        tools.invoke(7, "assessment.lookup", CoreValues.tree(Map.of("scope", "CATALOG")));
        tools.preview("assessment.lookup", CoreValues.tree(Map.of("scope", "ALL")));
        assertThatThrownBy(
                        () ->
                                tools.preview(
                                        "assessment.lookup",
                                        CoreValues.tree(Map.of("scope", "PERSONAL"))))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                StudentEvidenceTools.arguments(
                                        "assessment.lookup",
                                        CoreValues.tree(Map.of("scope", "OTHER"))))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(assessments, enrollments, evidence, submissions);
    }

    @Test
    void ownSummaryIsRealZeroAndChangeOfCurrentAuthorityInvalidatesIt() {
        when(assessments.readOwned(7, null)).thenReturn(List.of());
        var result =
                tools.invoke(7, "assessment.lookup", CoreValues.tree(Map.of("scope", "PERSONAL")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).data().path("savedResults").asInt()).isZero();
        assertThat(tools.available(7, result)).isTrue();
        when(assessments.activity(7, null))
                .thenReturn(
                        new edu.hcmute.careersim.assessment.dto.VersionedAssessmentContracts
                                .OwnActivity(null, 1, 1, 0, 0, 0));
        assertThat(tools.available(7, result)).isTrue();
        when(assessments.activity(7, null))
                .thenThrow(
                        new edu.hcmute.careersim.common.exception.ApiException(
                                edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN));
        assertThat(tools.available(7, result)).isFalse();
    }
}
