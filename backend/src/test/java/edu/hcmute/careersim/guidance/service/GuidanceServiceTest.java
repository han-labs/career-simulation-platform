package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.hcmute.careersim.assessment.access.AssessmentEvidenceAccess;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.guidance.dao.GuidanceDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.dto.SavePlanRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.service.impl.GuidanceServiceImpl;
import edu.hcmute.careersim.identity.access.StudentAccountAccess;
import edu.hcmute.careersim.simulation.access.SimulationEvidenceAccess;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuidanceServiceTest {

    private StudentAccountAccess accountAccess;
    private AssessmentEvidenceAccess assessmentAccess;
    private SimulationEvidenceAccess simulationAccess;
    private GuidanceDao guidanceDao;
    private SynGuidanceProvider synGuidanceProvider;
    private GuidanceService service;

    @BeforeEach
    void setUp() {
        accountAccess = mock(StudentAccountAccess.class);
        assessmentAccess = mock(AssessmentEvidenceAccess.class);
        simulationAccess = mock(SimulationEvidenceAccess.class);
        guidanceDao = mock(GuidanceDao.class);
        synGuidanceProvider = mock(SynGuidanceProvider.class);
        service =
                new GuidanceServiceImpl(
                        accountAccess,
                        assessmentAccess,
                        simulationAccess,
                        guidanceDao,
                        new StandardGuidancePolicy(),
                        List.of(synGuidanceProvider));
    }

    @Test
    void dashboardCombinesOnlyCompletedOwnedEvidence() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L))
                .thenReturn(
                        Optional.of(
                                new GuidanceDao.CurrentPlan(
                                        "Explore backend work",
                                        List.of("Complete another debugging task"),
                                        "ACTIVE")));

        var dashboard = service.getDashboard("student@example.test");

        assertThat(dashboard.studentName()).isEqualTo("Mai Student");
        assertThat(dashboard.progress().completed()).isEqualTo(3);
        assertThat(dashboard.interestSignals().get(0).code()).isEqualTo("I");
        assertThat(dashboard.recentResults().get(0).score()).isEqualTo(80);
        assertThat(dashboard.skills()).extracting("name").containsExactly("SQL querying");
        assertThat(dashboard.plan().nextStep()).isEqualTo("Complete another debugging task");
    }

    @Test
    void dashboardReturnsAValidEmptyStateWhenEvidenceDoesNotExist() {
        arrangeActiveStudent();
        when(assessmentAccess.findLatestCompleted(7L)).thenReturn(Optional.empty());
        when(simulationAccess.findRecentEvaluated(7L, 3)).thenReturn(List.of());
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var dashboard = service.getDashboard("student@example.test");

        assertThat(dashboard.progress().completed()).isZero();
        assertThat(dashboard.interestSignals()).isEmpty();
        assertThat(dashboard.recentResults()).isEmpty();
        assertThat(dashboard.plan()).isNull();
    }

    @Test
    void inactiveOrWrongRoleAccountCannotUseGuidance() {
        when(accountAccess.findByEmail("enterprise@example.test"))
                .thenReturn(
                        Optional.of(
                                new StudentAccountAccess.StudentAccount(
                                        8L, "Enterprise", "ENTERPRISE", "ACTIVE", null)));

        assertThatThrownBy(() -> service.getDashboard("enterprise@example.test"))
                .isInstanceOf(ApiException.class)
                .hasMessage("A student account is required.");
    }

    @Test
    void inactiveStudentCannotUseGuidance() {
        when(accountAccess.findByEmail("inactive@example.test"))
                .thenReturn(
                        Optional.of(
                                new StudentAccountAccess.StudentAccount(
                                        9L, "Inactive Student", "STUDENT", "LOCKED", null)));

        assertThatThrownBy(() -> service.getDashboard("inactive@example.test"))
                .isInstanceOf(ApiException.class)
                .hasMessage("The student account is not active.");
    }

    @Test
    void synReplyKeepsObjectiveScoreAndRecordsFallbackProvenance() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Review my latest result",
                                "REVIEW_LATEST",
                                new SynMessageRequest.Context(42L)));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        assertThat(response.resultCard().score()).isEqualTo(80);
        verify(guidanceDao)
                .recordGuidance(
                        eq(7L),
                        eq(11L),
                        eq(42L),
                        eq("REVIEW_LATEST"),
                        eq("FALLBACK"),
                        eq("READY"),
                        eq(null),
                        any(),
                        anyInt());
        verify(synGuidanceProvider, never()).generate(any(), any());
    }

    @Test
    void consentedFreeTextUsesAiAndRecordsModelProvenance() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.modelName()).thenReturn("test-model");
        when(synGuidanceProvider.generate(any(), any()))
                .thenReturn(
                        new SynGuidanceProvider.AiReply(
                                "Compare both experiences before deciding.",
                                List.of("What felt engaging?")));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "How do these experiences fit together?", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("AI");
        assertThat(response.text()).contains("Compare both experiences");
        verify(guidanceDao)
                .recordGuidance(
                        eq(7L),
                        eq(11L),
                        eq(42L),
                        eq("FREE_TEXT"),
                        eq("AI"),
                        eq("READY"),
                        eq("test-model"),
                        any(),
                        anyInt());
    }

    @Test
    void providerFailureReturnsStandardGuidanceWithoutChangingEvidence() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.generate(any(), any()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.TIMEOUT_OR_TRANSPORT));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("Help me reflect on this.", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        assertThat(response.resultCard()).isNull();
        verify(guidanceDao)
                .recordGuidance(
                        eq(7L),
                        eq(11L),
                        eq(42L),
                        eq("FREE_TEXT"),
                        eq("FALLBACK"),
                        eq("REPLACED_BY_FALLBACK"),
                        eq(null),
                        any(),
                        anyInt());
    }

    @Test
    void synRejectsAnAttemptOutsideOwnedCompletedEvidence() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.sendMessage(
                                        "student@example.test",
                                        new SynMessageRequest(
                                                "Review another result",
                                                "REVIEW_LATEST",
                                                new SynMessageRequest.Context(999L))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Completed simulation evidence was not found.");
    }

    @Test
    void planIsPersistedOnlyWhenSaveUseCaseIsCalled() {
        arrangeActiveStudent();
        when(guidanceDao.saveCurrentPlan(
                        7L,
                        "Explore backend work",
                        List.of("Complete one simulation", "Write a reflection")))
                .thenReturn(
                        new GuidanceDao.CurrentPlan(
                                "Explore backend work",
                                List.of("Complete one simulation", "Write a reflection"),
                                "ACTIVE"));

        var response =
                service.savePlan(
                        "student@example.test",
                        new SavePlanRequest(
                                " Explore backend work ",
                                List.of(" Complete one simulation ", "Write a reflection")));

        assertThat(response.saved()).isTrue();
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(guidanceDao)
                .saveCurrentPlan(
                        7L,
                        "Explore backend work",
                        List.of("Complete one simulation", "Write a reflection"));
    }

    private void arrangeActiveStudent() {
        when(accountAccess.findByEmail("student@example.test"))
                .thenReturn(
                        Optional.of(
                                new StudentAccountAccess.StudentAccount(
                                        7L, "Mai Student", "STUDENT", "ACTIVE", null)));
    }

    private void arrangeConsentedStudent() {
        when(accountAccess.findByEmail("student@example.test"))
                .thenReturn(
                        Optional.of(
                                new StudentAccountAccess.StudentAccount(
                                        7L,
                                        "Mai Student",
                                        "STUDENT",
                                        "ACTIVE",
                                        java.time.Instant.parse("2026-09-15T00:00:00Z"))));
    }

    private void arrangeEvidence() {
        when(assessmentAccess.findLatestCompleted(7L))
                .thenReturn(
                        Optional.of(
                                new AssessmentEvidenceAccess.AssessmentEvidence(
                                        11L,
                                        List.of(
                                                new AssessmentEvidenceAccess.DimensionScore(
                                                        "I", 18),
                                                new AssessmentEvidenceAccess.DimensionScore(
                                                        "C", 15)))));
        when(simulationAccess.findRecentEvaluated(7L, 3))
                .thenReturn(
                        List.of(
                                new SimulationEvidenceAccess.SimulationEvidence(
                                        42L,
                                        "Backend API Triage",
                                        80,
                                        List.of(
                                                new SimulationEvidenceAccess.TaskOutcome(
                                                        "SQL querying", "NEEDS_MORE_EVIDENCE")))));
    }
}
