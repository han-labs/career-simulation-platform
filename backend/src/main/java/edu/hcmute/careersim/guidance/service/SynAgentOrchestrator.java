package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.domain.SynAgentPolicy;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import edu.hcmute.careersim.guidance.dto.SynMessageRequest;
import edu.hcmute.careersim.guidance.dto.SynMessageResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Coordinates memory, deterministic tools, one optional provider call, and fallback. */
@Component
@Slf4j
public class SynAgentOrchestrator {

    private final SynAgentDao synAgentDao;
    private final SynAgentPolicy agentPolicy;
    private final SynAgentToolbox toolbox;
    private final StandardGuidancePolicy safetyPolicy;
    private final List<SynGuidanceProvider> providers;

    public SynAgentOrchestrator(
            SynAgentDao synAgentDao,
            SynAgentPolicy agentPolicy,
            SynAgentToolbox toolbox,
            StandardGuidancePolicy safetyPolicy,
            List<SynGuidanceProvider> providers) {
        this.synAgentDao = synAgentDao;
        this.agentPolicy = agentPolicy;
        this.toolbox = toolbox;
        this.safetyPolicy = safetyPolicy;
        this.providers = providers;
    }

    public AgentRun respond(
            long studentId,
            boolean consentedToAi,
            SynMessageRequest request,
            DashboardResponse dashboard,
            Long requestedAttemptId) {
        SynAgentDao.SessionMemory session =
                synAgentDao.loadOrCreateSession(studentId, request.sessionId());
        MemoryState memory = readMemory(session, dashboard);
        SynAgentPolicy.Decision decision =
                agentPolicy.decide(
                        request.resolvedActionType(),
                        request.message(),
                        session.lastPaths(),
                        safetyPolicy.requiresSafetyBoundary(request.message()),
                        request.resolvedResponseLanguage(),
                        memory.language());
        if (isNewStudent(dashboard)
                && "CAREER_QUESTION".equals(decision.intent())
                && decision.paths().isEmpty()) {
            decision =
                    agentPolicy.decide(
                            "GET_STARTED",
                            request.message(),
                            session.lastPaths(),
                            false,
                            request.resolvedResponseLanguage(),
                            memory.language());
        }
        SynAgentToolbox.ToolResult tools =
                toolbox.execute(decision, request.message(), dashboard, requestedAttemptId);

        String text = tools.standardText();
        List<String> suggestions = tools.suggestions();
        String provenance = "STANDARD";
        String auditSource = "FALLBACK";
        String auditStatus = "READY";
        String model = null;

        Optional<SynGuidanceProvider> provider =
                providers.stream().filter(SynGuidanceProvider::isAvailable).findFirst();
        boolean mayCallProvider =
                "FREE_TEXT".equals(request.resolvedActionType())
                        && consentedToAi
                        && provider.isPresent()
                        && !"SAFETY_BOUNDARY".equals(decision.intent());
        if (mayCallProvider) {
            SynGuidanceProvider selected = provider.orElseThrow();
            try {
                SynGuidanceProvider.AiReply reply =
                        selected.generate(
                                request.message(),
                                dashboard,
                                new SynGuidanceProvider.AgentContext(
                                        decision.intent(),
                                        decision.responseLanguage(),
                                        memory.stage(),
                                        decision.responseDepth(),
                                        session.summary(),
                                        memory.coveredTopics(),
                                        contextualObservations(memory, tools.observations())));
                text = reply.text();
                suggestions = reply.suggestions();
                provenance = "AI";
                auditSource = "AI";
                model = selected.modelName();
            } catch (SynGuidanceProvider.ProviderException exception) {
                auditStatus = "REPLACED_BY_FALLBACK";
                log.warn("Syn provider fallback category={}", exception.kind());
            }
        }

        List<String> rememberedPaths =
                tools.paths().isEmpty() ? session.lastPaths() : tools.paths();
        MemoryState updatedMemory = updateMemory(memory, decision, tools);
        synAgentDao.updateSession(
                studentId,
                session.id(),
                serialize(updatedMemory),
                decision.intent(),
                rememberedPaths);

        SynMessageResponse response =
                new SynMessageResponse(
                        UUID.randomUUID().toString(),
                        session.id(),
                        decision.intent(),
                        decision.responseLanguage(),
                        updatedMemory.stage(),
                        text,
                        provenance,
                        tools.activity(),
                        tools.evidenceReferences(),
                        tools.comparisonCard(),
                        tools.resourceCards(),
                        tools.resultCard(),
                        suggestions,
                        tools.planDraft());
        return new AgentRun(response, auditSource, auditStatus, model);
    }

    public boolean clearSession(long studentId, UUID sessionId) {
        return synAgentDao.clearSession(studentId, sessionId);
    }

    private MemoryState readMemory(SynAgentDao.SessionMemory session, DashboardResponse dashboard) {
        String language = null;
        String goal = session.lastIntent();
        List<String> covered = new ArrayList<>();
        String open = "NONE";
        String next = "NONE";
        int previousCompletedSteps = -1;
        String value = session.summary();
        if (value != null && value.startsWith("v=2|")) {
            for (String part : value.split("\\|")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) continue;
                switch (pair[0]) {
                    case "lang" -> language = safeCode(pair[1], null);
                    case "goal" -> goal = safeCode(pair[1], goal);
                    case "covered" -> {
                        if (!pair[1].isBlank()) {
                            for (String topic : pair[1].split(",")) {
                                String safe = safeCode(topic, null);
                                if (safe != null) covered.add(safe);
                            }
                        }
                    }
                    case "open" -> open = safeCode(pair[1], "NONE");
                    case "next" -> next = safeCode(pair[1], "NONE");
                    case "steps" -> previousCompletedSteps = safeInteger(pair[1], -1);
                    default -> {
                        // Ignore unknown versioned fields for forward compatibility.
                    }
                }
            }
        }
        return new MemoryState(
                language,
                explorationStage(dashboard),
                goal,
                List.copyOf(covered),
                open,
                next,
                previousCompletedSteps,
                dashboard.progress().completed());
    }

    private MemoryState updateMemory(
            MemoryState current,
            SynAgentPolicy.Decision decision,
            SynAgentToolbox.ToolResult tools) {
        LinkedHashSet<String> covered = new LinkedHashSet<>(current.coveredTopics());
        covered.add(decision.intent());
        List<String> boundedCovered =
                covered.stream().skip(Math.max(0, covered.size() - 6L)).toList();
        String nextAction =
                switch (decision.intent()) {
                    case "GET_STARTED" -> "CHOOSE_FIRST_EXPERIENCE";
                    case "COMPARE_PATHS" -> "TRY_COMPARISON_SIMULATION";
                    case "LEARNING_RESOURCES" -> "OPEN_ONE_REVIEWED_RESOURCE";
                    case "IDENTIFY_GAPS" -> "PRACTISE_ONE_EVIDENCE_GAP";
                    case "DRAFT_PLAN" -> "REVIEW_PLAN_DRAFT";
                    case "REVIEW_LATEST" -> "REFLECT_ON_RESULT";
                    default ->
                            tools.planDraft() == null
                                    ? current.proposedNextStep()
                                    : "REVIEW_PLAN_DRAFT";
                };
        return new MemoryState(
                decision.responseLanguage(),
                current.stage(),
                decision.intent(),
                boundedCovered,
                "FOLLOW_UP_" + decision.intent(),
                nextAction,
                current.previousCompletedSteps(),
                current.currentCompletedSteps());
    }

    private String serialize(MemoryState memory) {
        String value =
                "v=2|lang="
                        + safeCode(memory.language(), "EN")
                        + "|stage="
                        + safeCode(memory.stage(), "NEW")
                        + "|goal="
                        + safeCode(memory.currentGoal(), "GENERAL")
                        + "|covered="
                        + String.join(",", memory.coveredTopics())
                        + "|open="
                        + safeCode(memory.openQuestion(), "NONE")
                        + "|next="
                        + safeCode(memory.proposedNextStep(), "NONE");
        value += "|steps=" + memory.currentCompletedSteps();
        return value.substring(0, Math.min(value.length(), 600));
    }

    private List<String> contextualObservations(MemoryState memory, List<String> toolObservations) {
        List<String> values = new ArrayList<>(toolObservations);
        if (memory.previousCompletedSteps() >= 0
                && memory.currentCompletedSteps() > memory.previousCompletedSteps()) {
            values.add("Objective exploration progress increased since the previous Syn turn");
        }
        if (!"NONE".equals(memory.proposedNextStep())) {
            values.add("Previously proposed next action: " + memory.proposedNextStep());
        }
        return values.stream().limit(6).toList();
    }

    private String explorationStage(DashboardResponse dashboard) {
        if (dashboard.interestSignals().isEmpty() && dashboard.recentResults().isEmpty()) {
            return "NEW";
        }
        if (!dashboard.interestSignals().isEmpty() && dashboard.recentResults().isEmpty()) {
            return "ASSESSED";
        }
        if (dashboard.plan() == null) return "PRACTISING";
        return "PLANNING";
    }

    private boolean isNewStudent(DashboardResponse dashboard) {
        return dashboard.interestSignals().isEmpty() && dashboard.recentResults().isEmpty();
    }

    private String safeCode(String value, String fallback) {
        if (value == null) return fallback;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z0-9_]{1,60}") ? normalized : fallback;
    }

    private int safeInteger(String value, int fallback) {
        try {
            return Math.max(0, Math.min(Integer.parseInt(value), 3));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record MemoryState(
            String language,
            String stage,
            String currentGoal,
            List<String> coveredTopics,
            String openQuestion,
            String proposedNextStep,
            int previousCompletedSteps,
            int currentCompletedSteps) {}

    public record AgentRun(
            SynMessageResponse response,
            String auditSource,
            String auditStatus,
            String modelName) {}
}
