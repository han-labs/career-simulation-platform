package edu.hcmute.careersim.simulation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.enterprise.access.EnterpriseDirectoryAccess;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dao.*;
import edu.hcmute.careersim.simulation.domain.*;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.impl.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulationWorkflowTest {
    final EnrollmentDao enrollments = mock(EnrollmentDao.class);
    final LearningPolicyDao learning = mock(LearningPolicyDao.class);
    final ScenarioDao scenarios = mock(ScenarioDao.class);
    final CompletionDao records = mock(CompletionDao.class);
    final ActorAccess actors = mock(ActorAccess.class);
    final EnterpriseDirectoryAccess enterprises = mock(EnterpriseDirectoryAccess.class);
    final AuditWriter audit = mock(AuditWriter.class);
    final CompletionService completion = mock(CompletionService.class);
    final DeliverableValidator validator = mock(DeliverableValidator.class);
    final QuizTaskEvaluator quiz = mock(QuizTaskEvaluator.class);
    final Version version = mock(Version.class);
    final Task task = mock(Task.class);
    final Metadata metadata = mock(Metadata.class);
    final CompletionServiceImpl completionService =
            new CompletionServiceImpl(
                    enrollments, records, scenarios, learning, actors, enterprises, audit);
    final EnrollmentServiceImpl enrollmentService =
            new EnrollmentServiceImpl(enrollments, learning, scenarios, records, actors, audit);
    final TaskSubmissionServiceImpl submissions =
            new TaskSubmissionServiceImpl(
                    enrollments, learning, quiz, scenarios, actors, validator, completion, audit);

    @BeforeEach
    void setup() {
        var actor = new ActorAccess.Actor(7, "student@example.test", "Fixture", "STUDENT", 0, 0);
        when(actors.requireId(7)).thenReturn(actor);
        when(actors.lock(7)).thenReturn(actor);
        when(enrollments.owned(eq(7L), eq(2L), anyBoolean()))
                .thenReturn(
                        new EnrollmentDao.Row(
                                2, 7, 3, "IN_PROGRESS", 0, Instant.EPOCH, null, null));
        when(enrollments.completionId(2)).thenReturn(Optional.empty());
        when(enrollments.required(3)).thenReturn(1);
        when(enrollments.finalRequired(2, 3)).thenReturn(1);
        when(enrollments.finalTasks(2)).thenReturn(Set.of("4"));
        when(scenarios.version(3, false)).thenReturn(version);
        when(version.id()).thenReturn("3");
        when(version.scenarioId()).thenReturn("6");
        when(version.enterpriseId()).thenReturn("5");
        when(version.availabilityStatus()).thenReturn("AVAILABLE");
        when(version.tasks()).thenReturn(List.of(task));
        when(version.metadata()).thenReturn(metadata);
        when(scenarios.publicMetadata(metadata)).thenReturn(metadata);
        when(metadata.title()).thenReturn("Fixture simulation");
        when(task.id()).thenReturn("4");
        when(task.required()).thenReturn(true);
        when(task.taskType()).thenReturn("PRACTICAL");
        when(task.translations()).thenReturn(new ObjectMapper().createObjectNode());
        when(task.definitions()).thenReturn(List.of());
        when(task.resources()).thenReturn(List.of());
        when(learning.ordered(3)).thenReturn(true);
        when(learning.progress(2, 3)).thenReturn(List.of(new TaskProgress("4", true, true, false)));
        when(records.requiredSkills(2)).thenReturn(List.of("Unit testing"));
        when(enterprises.name(5)).thenReturn("Fixture enterprise");
    }

    @Test
    void finalSubmissionDoesNotCompleteAnOrderedSimulationBeforeLastReview() {
        completionService.completeIfEligible(7, 2);
        verify(records, never()).insert(anyLong(), any(), anyString(), anyString(), any());
    }

    @Test
    void completedReviewsCreateExactlyOneEligibleCompletion() {
        when(learning.progress(2, 3)).thenReturn(List.of(new TaskProgress("4", true, true, true)));
        when(records.insert(eq(2L), any(), anyString(), anyString(), any())).thenReturn(9L);
        completionService.completeIfEligible(7, 2);
        verify(records).complete(eq(2L), any());
        when(enrollments.completionId(2)).thenReturn(Optional.of("9"));
        completionService.completeIfEligible(7, 2);
        verify(records, times(1)).insert(eq(2L), any(), anyString(), anyString(), any());
    }

    @Test
    void historicalLegacyCompletionPolicyIsNotRewritten() {
        when(learning.ordered(3)).thenReturn(false);
        completionService.completeIfEligible(7, 2);
        verify(records).complete(eq(2L), any());
    }

    @Test
    void missingRequiredFinalCannotCompleteEvenIfReviewExists() {
        when(enrollments.finalRequired(2, 3)).thenReturn(0);
        completionService.completeIfEligible(7, 2);
        verifyNoInteractions(records);
    }

    @Test
    void resultsUseFinalSkillEvidenceAndNeverScorePracticalTasks() {
        var s =
                new EnrollmentDao.SubmissionRow(
                        8, 2, 4, 3, "FINAL", 1, UUID.randomUUID(), "hash", Instant.EPOCH);
        when(enrollments.submission(2, 4, false)).thenReturn(Optional.of(s));
        when(enrollments.practicedSkills(2)).thenReturn(List.of("Unit testing"));
        var result = enrollmentService.results(7, 2);
        assertThat(result.tasks().get(0).status()).isEqualTo("FINAL");
        assertThat(result.tasks().get(0).quizResult()).isNull();
        assertThat(result.practicedSkills()).containsExactly("Unit testing");
        assertThat(result.completion()).isNull();
        verify(learning, never()).score(8);
    }

    @Test
    void resultsPreserveARealQuizScoreOfZero() throws Exception {
        when(task.taskType()).thenReturn("QUIZ");
        when(enrollments.submission(2, 4, false))
                .thenReturn(
                        Optional.of(
                                new EnrollmentDao.SubmissionRow(
                                        8,
                                        2,
                                        4,
                                        3,
                                        "FINAL",
                                        1,
                                        UUID.randomUUID(),
                                        "hash",
                                        Instant.EPOCH)));
        when(learning.score(8))
                .thenReturn(new ObjectMapper().readTree("{\"correct\":0,\"total\":2}"));
        assertThat(
                        enrollmentService
                                .results(7, 2)
                                .tasks()
                                .get(0)
                                .quizResult()
                                .path("correct")
                                .asInt())
                .isZero();
    }

    @Test
    void resultsDenyForeignOwnersBeforeReadingSubmissionsOrSkills() {
        when(enrollments.owned(7, 2, false))
                .thenThrow(new NotFoundException("Enrollment not found"));
        assertThatThrownBy(() -> enrollmentService.results(7, 2))
                .isInstanceOf(NotFoundException.class);
        verify(enrollments, never()).practicedSkills(anyLong());
        verify(enrollments, never()).submission(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void repeatFinalReturnsExistingSubmissionWithoutAnotherWrite() {
        var request = UUID.randomUUID();
        var s =
                new EnrollmentDao.SubmissionRow(
                        8, 2, 4, 3, "FINAL", 1, request, "hash", Instant.EPOCH);
        when(enrollments.draft(any(), eq(4L))).thenReturn(s);
        when(validator.validate(eq(7L), anyList(), anyList(), eq(true))).thenReturn(List.of());
        when(validator.hash(eq(7L), eq(2L), eq(4L), eq(3L), anyList())).thenReturn("hash");
        assertThat(submissions.finalizeTask(7, 2, 4, 0, new Finalize(request, List.of())).status())
                .isEqualTo("FINAL");
        verify(enrollments, never()).save(any(), anyList());
        verify(enrollments, never()).finalizeTask(anyLong(), any(), anyString());
    }

    @Test
    void selfReviewRechecksCompletionAndStillRequiresFinal() {
        var s =
                new EnrollmentDao.SubmissionRow(
                        8, 2, 4, 3, "FINAL", 1, UUID.randomUUID(), "hash", Instant.EPOCH);
        when(enrollments.submission(2, 4, true)).thenReturn(Optional.of(s));
        when(learning.reviewed(8)).thenReturn(Instant.EPOCH);
        assertThat(submissions.selfReview(7, 2, 4).reviewedAt()).isNotNull();
        verify(learning).review(8, 7);
        verify(completion).completeIfEligible(7, 2);
        when(enrollments.submission(2, 4, true)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> submissions.selfReview(7, 2, 4)).isInstanceOf(ApiException.class);
        verify(completion, times(1)).completeIfEligible(7, 2);
    }

    @Test
    void modelAnswersStayLockedWithoutAValidFinal() {
        when(enrollments.submission(2, 4, false)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> submissions.readAnswer(7, 2, 4)).isInstanceOf(ApiException.class);
    }

    @Test
    void missingProgressCannotAccidentallyAuthorizeCompletion() {
        when(learning.progress(2, 3)).thenReturn(List.of());
        completionService.completeIfEligible(7, 2);
        verifyNoInteractions(records);
    }

    @Test
    void realQuizEvaluatorReturnsZeroAndPracticalWorkHasNoScore() throws Exception {
        var mapper = new ObjectMapper();
        when(task.rubric())
                .thenReturn(
                        new Rubric(
                                1,
                                "quiz-exact",
                                mapper.readTree(
                                        "{\"definitionPosition\":1,\"answers\":{\"choice\":\"A\"}}")));
        var definition = mock(Definition.class);
        when(definition.id()).thenReturn("9");
        when(definition.position()).thenReturn(1);
        when(task.definitions()).thenReturn(List.of(definition));
        var evaluator = new QuizTaskEvaluator();
        var score =
                evaluator.evaluate(
                        task,
                        List.of(
                                new Item(
                                        "9",
                                        null,
                                        null,
                                        null,
                                        mapper.readTree("{\"choice\":\"B\"}"))));
        assertThat(score.path("correct").asInt()).isZero();
        assertThat(score.path("total").asInt()).isEqualTo(1);
        when(task.rubric()).thenReturn(null);
        assertThat(evaluator.evaluate(task, List.of())).isNull();
    }

    @Test
    void realValidatorRejectsAnIncompleteRequiredFinalResponse() throws Exception {
        var definition =
                new Definition(
                        "9",
                        1,
                        "Response",
                        "TEXT",
                        true,
                        new ObjectMapper()
                                .readTree(
                                        "{\"schemaVersion\":1,\"minLength\":1,\"maxLength\":100}"));
        var realValidator =
                new DeliverableValidator(
                        mock(edu.hcmute.careersim.common.storage.FileAssetService.class));
        assertThatThrownBy(() -> realValidator.validate(7, List.of(definition), List.of(), true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void privateResultsRejectANonStudentRole() {
        when(actors.requireId(7))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "admin@example.test", "Admin", "ADMINISTRATOR", 0, 0));
        assertThatThrownBy(() -> enrollmentService.results(7, 2)).isInstanceOf(ApiException.class);
        verify(enrollments, never()).owned(anyLong(), anyLong(), anyBoolean());
    }
}
