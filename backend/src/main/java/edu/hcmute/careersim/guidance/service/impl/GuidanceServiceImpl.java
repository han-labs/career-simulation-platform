package edu.hcmute.careersim.guidance.service.impl;

import edu.hcmute.careersim.assessment.access.AssessmentEvidenceAccess;
import edu.hcmute.careersim.assessment.access.AssessmentEvidenceAccess.AssessmentEvidence;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.guidance.dao.GuidanceDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.PlanResponse;
import edu.hcmute.careersim.guidance.dto.SavePlanRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import edu.hcmute.careersim.guidance.service.GuidanceService;
import edu.hcmute.careersim.guidance.service.SynGuidanceProvider;
import edu.hcmute.careersim.identity.access.StudentAccountAccess;
import edu.hcmute.careersim.identity.access.StudentAccountAccess.StudentAccount;
import edu.hcmute.careersim.simulation.access.SimulationEvidenceAccess;
import edu.hcmute.careersim.simulation.access.SimulationEvidenceAccess.SimulationEvidence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuidanceServiceImpl implements GuidanceService {

    private static final Map<String, String> RIASEC_LABELS =
            Map.of(
                    "R", "Realistic",
                    "I", "Investigative",
                    "A", "Artistic",
                    "S", "Social",
                    "E", "Enterprising",
                    "C", "Conventional");

    private final StudentAccountAccess studentAccountAccess;
    private final AssessmentEvidenceAccess assessmentEvidenceAccess;
    private final SimulationEvidenceAccess simulationEvidenceAccess;
    private final GuidanceDao guidanceDao;
    private final StandardGuidancePolicy standardGuidancePolicy;
    private final List<SynGuidanceProvider> synGuidanceProviders;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String authenticatedEmail) {
        StudentAccount student = requireActiveStudent(authenticatedEmail);
        return buildDashboard(student);
    }

    @Override
    public SynMessageResponse sendMessage(String authenticatedEmail, SynMessageRequest request) {
        long startedAt = System.nanoTime();
        StudentAccount student = requireActiveStudent(authenticatedEmail);
        DashboardResponse dashboard = buildDashboard(student);
        Long requestedAttemptId = request.context() == null ? null : request.context().attemptId();

        if (requestedAttemptId != null
                && dashboard.recentResults().stream()
                        .noneMatch(result -> result.id() == requestedAttemptId)) {
            throw new NotFoundException("Completed simulation evidence was not found.");
        }

        StandardGuidancePolicy.DraftReply draft =
                standardGuidancePolicy.createReply(
                        request.resolvedActionType(),
                        request.message(),
                        dashboard,
                        requestedAttemptId);
        SynMessageResponse response =
                new SynMessageResponse(
                        UUID.randomUUID().toString(),
                        draft.text(),
                        "STANDARD",
                        draft.resultCard(),
                        draft.suggestions(),
                        draft.planDraft());
        String auditSource = "FALLBACK";
        String auditStatus = "READY";
        String auditModel = null;

        Optional<SynGuidanceProvider> availableProvider =
                synGuidanceProviders.stream().filter(SynGuidanceProvider::isAvailable).findFirst();
        boolean eligibleForAi =
                "FREE_TEXT".equals(request.resolvedActionType())
                        && student.consentedToAiAt() != null
                        && availableProvider.isPresent()
                        && !standardGuidancePolicy.requiresSafetyBoundary(request.message());
        if (eligibleForAi) {
            SynGuidanceProvider selectedProvider = availableProvider.orElseThrow();
            try {
                SynGuidanceProvider.AiReply aiReply =
                        selectedProvider.generate(request.message(), dashboard);
                response =
                        new SynMessageResponse(
                                UUID.randomUUID().toString(),
                                aiReply.text(),
                                "AI",
                                null,
                                aiReply.suggestions(),
                                null);
                auditSource = "AI";
                auditModel = selectedProvider.modelName();
            } catch (SynGuidanceProvider.ProviderException exception) {
                auditStatus = "REPLACED_BY_FALLBACK";
                log.warn("Syn provider fallback category={}", exception.kind());
            }
        }

        Optional<AssessmentEvidence> assessment =
                assessmentEvidenceAccess.findLatestCompleted(student.id());
        Long simulationAttemptId =
                requestedAttemptId != null
                        ? requestedAttemptId
                        : dashboard.recentResults().stream()
                                .findFirst()
                                .map(DashboardResponse.RecentResult::id)
                                .orElse(null);
        Long assessmentAttemptId = assessment.map(AssessmentEvidence::attemptId).orElse(null);
        if (assessmentAttemptId != null || simulationAttemptId != null) {
            int generationMs =
                    (int)
                            Math.min(
                                    Integer.MAX_VALUE,
                                    Math.max(0, (System.nanoTime() - startedAt) / 1_000_000));
            guidanceDao.recordGuidance(
                    student.id(),
                    assessmentAttemptId,
                    simulationAttemptId,
                    request.resolvedActionType(),
                    auditSource,
                    auditStatus,
                    auditModel,
                    response,
                    generationMs);
        }
        return response;
    }

    @Override
    @Transactional
    public PlanResponse savePlan(String authenticatedEmail, SavePlanRequest request) {
        StudentAccount student = requireActiveStudent(authenticatedEmail);
        GuidanceDao.CurrentPlan saved =
                guidanceDao.saveCurrentPlan(
                        student.id(), request.title().trim(), sanitizedSteps(request.steps()));
        return new PlanResponse(saved.title(), saved.steps(), saved.status(), true);
    }

    private StudentAccount requireActiveStudent(String email) {
        StudentAccount student =
                studentAccountAccess
                        .findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("Student account was not found."));
        if (!"STUDENT".equals(student.role())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "A student account is required.");
        }
        if (!"ACTIVE".equals(student.status())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "The student account is not active.");
        }
        return student;
    }

    private DashboardResponse buildDashboard(StudentAccount student) {
        Optional<AssessmentEvidence> assessment =
                assessmentEvidenceAccess.findLatestCompleted(student.id());
        List<SimulationEvidence> simulations =
                simulationEvidenceAccess.findRecentEvaluated(student.id(), 3);
        Optional<GuidanceDao.CurrentPlan> plan = guidanceDao.findCurrentPlan(student.id());

        List<DashboardResponse.InterestSignal> signals =
                assessment.stream()
                        .flatMap(value -> value.scores().stream())
                        .sorted(
                                Comparator.comparingInt(
                                                AssessmentEvidenceAccess.DimensionScore::score)
                                        .reversed())
                        .limit(3)
                        .map(
                                score ->
                                        new DashboardResponse.InterestSignal(
                                                score.dimension(),
                                                RIASEC_LABELS.getOrDefault(
                                                        score.dimension(), score.dimension()),
                                                score.score()))
                        .toList();

        List<DashboardResponse.RecentResult> recentResults =
                simulations.stream().map(this::toRecentResult).toList();
        List<DashboardResponse.SkillSummary> skills = summarizeSkills(simulations);
        DashboardResponse.CurrentPlan currentPlan =
                plan.map(
                                value ->
                                        new DashboardResponse.CurrentPlan(
                                                value.title(),
                                                value.steps().isEmpty() ? "" : value.steps().get(0),
                                                value.status()))
                        .orElse(null);

        int completed =
                (assessment.isPresent() ? 1 : 0)
                        + (!simulations.isEmpty() ? 1 : 0)
                        + (plan.isPresent() ? 1 : 0);
        String progressLabel =
                completed == 0
                        ? "Start your exploration"
                        : completed + " of 3 exploration steps completed";

        return new DashboardResponse(
                "LIVE",
                student.displayName(),
                new DashboardResponse.Progress(completed, 3, progressLabel),
                signals,
                skills,
                recentResults,
                currentPlan);
    }

    private DashboardResponse.RecentResult toRecentResult(SimulationEvidence simulation) {
        List<DashboardResponse.ResultOutcome> outcomes =
                simulation.outcomes().stream()
                        .map(
                                outcome ->
                                        new DashboardResponse.ResultOutcome(
                                                outcome.label(), outcome.status()))
                        .toList();
        return new DashboardResponse.RecentResult(
                simulation.attemptId(),
                simulation.title(),
                simulation.score(),
                "Completed simulation",
                outcomes);
    }

    private List<DashboardResponse.SkillSummary> summarizeSkills(
            List<SimulationEvidence> simulations) {
        Map<String, DashboardResponse.SkillSummary> summaries = new LinkedHashMap<>();
        for (SimulationEvidence simulation : simulations) {
            for (SimulationEvidenceAccess.TaskOutcome outcome : simulation.outcomes()) {
                summaries.putIfAbsent(
                        outcome.label(),
                        new DashboardResponse.SkillSummary(
                                outcome.label(), outcome.status(), simulation.title()));
                if (summaries.size() == 3) {
                    return List.copyOf(summaries.values());
                }
            }
        }
        return new ArrayList<>(summaries.values());
    }

    private List<String> sanitizedSteps(List<String> steps) {
        return steps.stream().map(String::trim).toList();
    }
}
