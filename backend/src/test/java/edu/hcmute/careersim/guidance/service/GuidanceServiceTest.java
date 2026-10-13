package edu.hcmute.careersim.guidance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.hcmute.careersim.assessment.access.AssessmentEvidenceAccess;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.guidance.dao.GuidanceDao;
import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.domain.SynAgentPolicy;
import edu.hcmute.careersim.guidance.domain.SynToolIdentifier;
import edu.hcmute.careersim.guidance.dto.SavePlanRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.service.impl.GuidanceServiceImpl;
import edu.hcmute.careersim.identity.access.StudentAccountAccess;
import edu.hcmute.careersim.simulation.access.SimulationEvidenceAccess;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuidanceServiceTest {

    private StudentAccountAccess accountAccess;
    private AssessmentEvidenceAccess assessmentAccess;
    private SimulationEvidenceAccess simulationAccess;
    private GuidanceDao guidanceDao;
    private SynAgentDao synAgentDao;
    private SynGuidanceProvider synGuidanceProvider;
    private GuidanceService service;
    private edu.hcmute.careersim.guidance.dao.SynTurnDao turns;

    @BeforeEach
    void setUp() {
        accountAccess = mock(StudentAccountAccess.class);
        assessmentAccess = mock(AssessmentEvidenceAccess.class);
        simulationAccess = mock(SimulationEvidenceAccess.class);
        guidanceDao = mock(GuidanceDao.class);
        synAgentDao = mock(SynAgentDao.class);
        synGuidanceProvider = mock(SynGuidanceProvider.class);
        StandardGuidancePolicy standardPolicy = new StandardGuidancePolicy();
        SynAgentToolbox toolbox = new SynAgentToolbox(synAgentDao, standardPolicy);
        SynAgentOrchestrator orchestrator =
                new SynAgentOrchestrator(
                        synAgentDao,
                        new SynAgentPolicy(),
                        toolbox,
                        standardPolicy,
                        List.of(synGuidanceProvider));
        when(synAgentDao.loadOrCreateSession(eq(7L), nullable(UUID.class)))
                .thenReturn(new SynAgentDao.SessionMemory(UUID.randomUUID(), "", null, List.of()));
        when(synAgentDao.findSession(eq(7L), any(UUID.class)))
                .thenAnswer(
                        call ->
                                Optional.of(
                                        synAgentDao.loadOrCreateSession(7L, call.getArgument(1))));
        when(synAgentDao.findPaths(any())).thenReturn(List.of());
        when(synAgentDao.findResources(any(), any(), anyInt())).thenReturn(List.of());
        when(synAgentDao.findSimulations(any(), anyInt())).thenReturn(List.of());
        turns = mock(edu.hcmute.careersim.guidance.dao.SynTurnDao.class);
        when(turns.find(eq(7L), any()))
                .thenAnswer(
                        call ->
                                Optional.of(
                                        new edu.hcmute.careersim.guidance.dao.SynTurnDao.Turn(
                                                call.getArgument(1),
                                                7L,
                                                UUID.randomUUID(),
                                                "hash",
                                                UUID.randomUUID(),
                                                0,
                                                "PENDING",
                                                null,
                                                false)));
        SynTurnLifecycleService lifecycle =
                new SynTurnLifecycleService(
                        turns,
                        synAgentDao,
                        accountAccess,
                        mock(edu.hcmute.careersim.guidance.dao.KnowledgeDao.class),
                        new edu.hcmute.careersim.guidance.service.SynUsagePricing(
                                new com.fasterxml.jackson.databind.ObjectMapper(), "[]"));
        service =
                new GuidanceServiceImpl(
                        accountAccess,
                        assessmentAccess,
                        simulationAccess,
                        guidanceDao,
                        orchestrator,
                        lifecycle);
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
        when(simulationAccess.findRecentEvaluated(7L, 10)).thenReturn(List.of());
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var dashboard = service.getDashboard("student@example.test");

        assertThat(dashboard.progress().completed()).isZero();
        assertThat(dashboard.interestSignals()).isEmpty();
        assertThat(dashboard.recentResults()).isEmpty();
        assertThat(dashboard.plan()).isNull();
    }

    @Test
    void networkUseCaseIsOutsideTheShortFinalizationTransaction() throws Exception {
        var method =
                GuidanceServiceImpl.class.getMethod(
                        "sendMessage", String.class, SynMessageRequest.class);

        assertThat(
                        method.isAnnotationPresent(
                                org.springframework.transaction.annotation.Transactional.class))
                .isFalse();
        assertThat(
                        SynTurnLifecycleService.class
                                .getMethod(
                                        "finish",
                                        String.class,
                                        edu.hcmute.careersim.guidance.dao.SynTurnDao.Turn.class,
                                        SynAgentOrchestrator.AgentRun.class,
                                        int.class)
                                .getAnnotation(
                                        org.springframework.transaction.annotation.Transactional
                                                .class)
                                .timeout())
                .isEqualTo(3);
    }

    @Test
    void boundedAgentAllowsExactlyTwoProviderPasses() {
        assertThat(SynAgentOrchestrator.MAX_PROVIDER_PASSES).isEqualTo(2);
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
        verify(turns)
                .complete(
                        eq(7L),
                        any(UUID.class),
                        any(),
                        eq("NO_AI_CONSENT"),
                        eq(null),
                        anyInt(),
                        any());
        verify(synGuidanceProvider, never()).generate(any(), any(), any());
    }

    @Test
    void consentedFreeTextUsesAiAndRecordsModelProvenance() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.modelName()).thenReturn("test-model");
        when(synGuidanceProvider.generate(any(), any(), any()))
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
        verify(turns)
                .complete(
                        eq(7L),
                        any(UUID.class),
                        any(),
                        eq("AI_READY"),
                        eq("test-model"),
                        anyInt(),
                        any());
    }

    @Test
    void consentedGeminiFlowUsesExactlyTwoBoundedProviderPasses() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.supportsToolCalling()).thenReturn(true);
        when(synGuidanceProvider.modelName()).thenReturn("test-gemini-model");
        when(synGuidanceProvider.requestTools(any(), any(), anyList()))
                .thenReturn(
                        new SynGuidanceProvider.ToolPlan(
                                List.of(SynToolIdentifier.FIND_RESOURCES)));
        when(synGuidanceProvider.synthesize(any(), any(), any(), any(), any()))
                .thenReturn(
                        new SynGuidanceProvider.AiReply(
                                "Use one reviewed resource, then test it in practice.",
                                List.of("Show me the resource")));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "How should I continue exploring?", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("AI");
        assertThat(response.text()).contains("reviewed resource");
        verify(synGuidanceProvider).requestTools(any(), any(), anyList());
        verify(synGuidanceProvider).synthesize(any(), any(), any(), any(), any());
        verify(synGuidanceProvider, never()).generate(any(), any(), any());
        verify(synAgentDao).findResources(any(), any(), anyInt());
        verify(synAgentDao).findSimulations(any(), anyInt());
    }

    @Test
    void secondPassToolRequestFallsBackToDeterministicPlan() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.supportsToolCalling()).thenReturn(true);
        when(synGuidanceProvider.requestTools(any(), any(), anyList()))
                .thenReturn(
                        new SynGuidanceProvider.ToolPlan(List.of(SynToolIdentifier.READ_EVIDENCE)));
        when(synGuidanceProvider.synthesize(any(), any(), any(), any(), any()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.ITERATION_LIMIT));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "How should I continue exploring?", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        verify(synGuidanceProvider).requestTools(any(), any(), anyList());
        verify(synGuidanceProvider).synthesize(any(), any(), any(), any(), any());
        verify(synGuidanceProvider, never()).generate(any(), any(), any());
        verify(synAgentDao, times(2)).findSimulations(any(), anyInt());
        verify(turns)
                .complete(
                        eq(7L),
                        any(UUID.class),
                        any(),
                        eq("ITERATION_LIMIT"),
                        eq(null),
                        anyInt(),
                        any());
    }

    @Test
    void allLlmSelectedToolsDegradingSkipsSynthesisAndUsesSafetyPlan() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.supportsToolCalling()).thenReturn(true);
        when(synGuidanceProvider.requestTools(any(), any(), anyList()))
                .thenReturn(
                        new SynGuidanceProvider.ToolPlan(List.of(SynToolIdentifier.COMPARE_PATHS)));
        when(synAgentDao.findPaths(any()))
                .thenThrow(new IllegalStateException("synthetic comparison failure"));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("Compare backend and frontend", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        assertThat(response.activity())
                .anyMatch(activity -> activity.contains("Could not complete compare paths"));
        verify(synGuidanceProvider).requestTools(any(), any(), anyList());
        verify(synGuidanceProvider, never()).synthesize(any(), any(), any(), any(), any());
        verify(synGuidanceProvider, never()).generate(any(), any(), any());
    }

    @Test
    void providerFailureReturnsStandardGuidanceWithoutChangingEvidence() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.generate(any(), any(), any()))
                .thenThrow(
                        new SynGuidanceProvider.ProviderException(
                                SynGuidanceProvider.FailureKind.TIMEOUT_OR_TRANSPORT));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("Help me reflect on this.", "FREE_TEXT", null));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        assertThat(response.resultCard()).isNull();
        verify(turns)
                .complete(
                        eq(7L),
                        any(UUID.class),
                        any(),
                        eq("TIMEOUT_OR_TRANSPORT"),
                        eq(null),
                        anyInt(),
                        any());
    }

    @Test
    void oneToolFailureIsIsolatedAndRemainingStandardFlowStillReturns() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synAgentDao.findPaths(any()))
                .thenThrow(new IllegalStateException("synthetic path lookup failure"));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Compare backend and frontend", "COMPARE_PATHS", null));

        assertThat(response.provenance()).isEqualTo("STANDARD");
        assertThat(response.comparisonCard()).isNull();
        assertThat(response.activity())
                .anyMatch(activity -> activity.contains("Could not complete compare paths"));
        assertThat(response.resultCards()).isNotEmpty();
        verify(synAgentDao).updateSession(eq(7L), any(UUID.class), any(), any(), any());
    }

    @Test
    void newVietnameseStudentReceivesOnboardingAndBoundedStructuredMemory() {
        arrangeActiveStudent();
        when(assessmentAccess.findLatestCompleted(7L)).thenReturn(Optional.empty());
        when(simulationAccess.findRecentEvaluated(7L, 10)).thenReturn(List.of());
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "em moi vao, chua biet bat dau ntn",
                                "FREE_TEXT",
                                null,
                                "AUTO",
                                null));

        assertThat(response.intent()).isEqualTo("GET_STARTED");
        assertThat(response.responseLanguage()).isEqualTo("VI");
        assertThat(response.explorationStage()).isEqualTo("NEW");
        assertThat(response.text()).contains("chưa cần");
        verify(synAgentDao)
                .updateSession(
                        eq(7L),
                        any(UUID.class),
                        org.mockito.ArgumentMatchers.argThat(
                                summary ->
                                        summary.startsWith("v=2|")
                                                && summary.contains("lang=VI")
                                                && summary.contains("goal=GET_STARTED")),
                        eq("GET_STARTED"),
                        eq(List.of()));
        verify(synGuidanceProvider, never()).generate(any(), any(), any());
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
    void generalFreeTextComparisonDoesNotDumpPersonalResults() {
        arrangeActiveStudent();
        arrangeAssessment();
        when(simulationAccess.findRecentEvaluated(7L, 10))
                .thenReturn(
                        List.of(
                                simulation(45L, "Backend Developer", 82),
                                simulation(44L, "Data Quality Investigation", 67),
                                simulation(43L, "Frontend Accessibility Review", 100),
                                simulation(42L, "Backend Developer", 100)));
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("so sanh FE vs BE giup minh", "FREE_TEXT", null));

        assertThat(response.resultCards()).isEmpty();
        assertThat(response.evidenceReferences()).isEmpty();
        assertThat(response.simulationCards()).isEmpty();
        assertThat(response.comparisonCard()).isNotNull();
        assertThat(response.comparisonCard().paths())
                .allSatisfy(
                        path -> assertThat(path.evidenceSummary()).doesNotContain("No completed"));
    }

    @Test
    void comparisonUsesReviewedPathSkillAndSimulationMetadata() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());
        when(synAgentDao.findPaths(any()))
                .thenReturn(
                        List.of(
                                new SynAgentDao.PathProfile(
                                        "BACKEND_DEVELOPMENT",
                                        "Backend development",
                                        "Build and diagnose server-side APIs.",
                                        "IRC",
                                        List.of("API design"),
                                        List.of("api_reasoning", "sql")),
                                new SynAgentDao.PathProfile(
                                        "FRONTEND_DEVELOPMENT",
                                        "Frontend development",
                                        "Create accessible browser experiences.",
                                        "AIC",
                                        List.of("Accessibility review"),
                                        List.of("accessibility"))));
        when(synAgentDao.findSkillEvidence(any()))
                .thenReturn(
                        List.of(
                                new SynAgentDao.SkillEvidence(
                                        42L,
                                        "BACKEND_DEVELOPMENT",
                                        "sql",
                                        "SQL querying",
                                        "OBSERVED_STRENGTH")));
        when(synAgentDao.findSimulations(any(), anyInt()))
                .thenReturn(
                        List.of(
                                new SynAgentDao.SimulationOption(
                                        "Backend Developer",
                                        "BACKEND_DEVELOPMENT",
                                        "Investigate API and database behavior.",
                                        "INTRODUCTORY",
                                        35)));

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Compare backend and frontend", "COMPARE_PATHS", null));

        assertThat(response.comparisonCard().paths().get(0).evidenceSummary())
                .contains("Build and diagnose server-side APIs", "SQL querying");
        assertThat(response.simulationCards())
                .singleElement()
                .satisfies(
                        simulation -> {
                            assertThat(simulation.summary())
                                    .isEqualTo("Investigate API and database behavior.");
                            assertThat(simulation.difficulty()).isEqualTo("INTRODUCTORY");
                            assertThat(simulation.estimatedMinutes()).isEqualTo(35);
                        });
    }

    @Test
    void historySelectionReturnsTheRequestedResultInsteadOfTheLatestOne() {
        arrangeActiveStudent();
        arrangeAssessment();
        when(simulationAccess.findRecentEvaluated(7L, 10))
                .thenReturn(
                        List.of(
                                simulation(44L, "Data Quality Investigation", 67),
                                simulation(43L, "Frontend Accessibility Review", 100),
                                simulation(42L, "Backend Developer", 100)));
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Review my Frontend Accessibility Review result",
                                "REVIEW_RESULT",
                                new SynMessageRequest.Context(43L)));

        assertThat(response.resultCard().id()).isEqualTo(43L);
        assertThat(response.resultCard().title()).isEqualTo("Frontend Accessibility Review");
        assertThat(response.evidenceReferences())
                .extracting("label")
                .contains("Frontend Accessibility Review result")
                .doesNotContain("Data Quality Investigation result");
    }

    @Test
    void conversationalSaveRequestNeverPersistsOrClaimsSuccess() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(guidanceDao.findCurrentPlan(7L)).thenReturn(Optional.empty());

        var response =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("save it", "FREE_TEXT", null));

        assertThat(response.intent()).isEqualTo("CONFIRM_PLAN");
        assertThat(response.text()).contains("has not been saved");
        verify(guidanceDao, never()).saveCurrentPlan(anyLong(), any(), any());
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

    @Test
    void anOwnedCompletedAttemptOutsideRecentResultsIsStillAccessible() {
        arrangeActiveStudent();
        arrangeEvidence();
        when(simulationAccess.findOwnedEvaluated(7L, 12L))
                .thenReturn(Optional.of(simulation(12L, "Backend historic fixture", 65)));
        var reply =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Review this result",
                                "REVIEW_RESULT",
                                new SynMessageRequest.Context(12L)));
        assertThat(reply.resultCard().id()).isEqualTo(12L);
        assertThat(reply.resultCard().score()).isEqualTo(65);
    }

    @Test
    void secondPlanStepUsesServerStoredDraftInModelContext() {
        arrangeConsentedStudent();
        arrangeEvidence();
        var plan =
                new edu.hcmute.careersim.guidance.dto.SynMessageResponse.PlanDraft(
                        "Explore", List.of("Practise", "Reflect on evidence", "Compare"));
        when(synAgentDao.pendingPlan(eq(7L), any())).thenReturn(plan);
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.generate(any(), any(), any()))
                .thenReturn(
                        new SynGuidanceProvider.AiReply(
                                "Reflect on the evidence from your practice.", List.of()));
        var reply =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest(
                                "Explain the second step of that plan", "FREE_TEXT", null));
        assertThat(reply.text()).isEqualTo("Reflect on the evidence from your practice.");
        verify(synGuidanceProvider)
                .generate(
                        any(), any(), argThat(c -> plan.equals(c.toolData().get("PENDING_PLAN"))));
    }

    @Test
    void fabricatedNumericScoreIsReplacedByStandardGuidance() {
        arrangeConsentedStudent();
        arrangeEvidence();
        when(synGuidanceProvider.isAvailable()).thenReturn(true);
        when(synGuidanceProvider.generate(any(), any(), any()))
                .thenReturn(new SynGuidanceProvider.AiReply("Your score is 100/100.", List.of()));
        var reply =
                service.sendMessage(
                        "student@example.test",
                        new SynMessageRequest("Review my score", "FREE_TEXT", null));
        assertThat(reply.provenance()).isEqualTo("STANDARD");
        assertThat(reply.resultCard().score()).isEqualTo(80);
        assertThat(reply.text()).doesNotContain("100/100");
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
        arrangeAssessment();
        when(simulationAccess.findRecentEvaluated(7L, 10))
                .thenReturn(List.of(simulation(42L, "Backend API Triage", 80)));
    }

    private void arrangeAssessment() {
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
    }

    private SimulationEvidenceAccess.SimulationEvidence simulation(
            long id, String title, int score) {
        String careerTrack =
                title.contains("Data")
                        ? "DATA_ANALYSIS"
                        : title.contains("Frontend")
                                ? "FRONTEND_DEVELOPMENT"
                                : "BACKEND_DEVELOPMENT";
        return new SimulationEvidenceAccess.SimulationEvidence(
                id,
                title,
                score,
                List.of(
                        new SimulationEvidenceAccess.TaskOutcome(
                                title.contains("Data")
                                        ? "Handle duplicate records"
                                        : title.contains("Backend")
                                                ? "SQL querying"
                                                : "Relevant task evidence",
                                title.contains("Data")
                                        ? "NEEDS_MORE_EVIDENCE"
                                        : "OBSERVED_STRENGTH")),
                java.time.Instant.parse("2026-09-17T00:00:00Z").plusSeconds(id),
                careerTrack);
    }
}
