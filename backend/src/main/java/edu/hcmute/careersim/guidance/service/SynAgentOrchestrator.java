package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.SynAgentDao;
import edu.hcmute.careersim.guidance.domain.StandardGuidancePolicy;
import edu.hcmute.careersim.guidance.domain.SynAgentPolicy;
import edu.hcmute.careersim.guidance.domain.SynIntent;
import edu.hcmute.careersim.guidance.domain.SynToolIdentifier;
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

/** Coordinates memory, bounded provider tool calling, deterministic tools, and fallback. */
@Component
@Slf4j
public class SynAgentOrchestrator {

    static final int MAX_PROVIDER_PASSES = 2;
    private static final int MAX_TOOLS_PER_PLAN = 8;

    private final SynAgentDao synAgentDao;
    private final SynAgentPolicy agentPolicy;
    private final SynAgentToolbox toolbox;
    private final StandardGuidancePolicy safetyPolicy;
    private final List<SynGuidanceProvider> providers;
    private KnowledgeRetrievalService knowledge;

    @org.springframework.beans.factory.annotation.Autowired
    public void setKnowledge(KnowledgeRetrievalService knowledge) {
        this.knowledge = knowledge;
    }

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
                synAgentDao
                        .findSession(studentId, request.sessionId())
                        .orElseThrow(
                                () ->
                                        new edu.hcmute.careersim.common.exception.ApiException(
                                                edu.hcmute.careersim.common.exception.ErrorCode
                                                        .CONFLICT,
                                                "Conversation expired or cleared."));
        MemoryState memory = readMemory(session, dashboard);
        var pendingPlan = synAgentDao.pendingPlan(studentId, session.id());
        SynAgentPolicy.Decision decision =
                agentPolicy.decide(
                        request.resolvedActionType(),
                        request.message(),
                        session.lastPaths(),
                        safetyPolicy.requiresSafetyBoundary(request.message()),
                        request.resolvedResponseLanguage(),
                        memory.language());
        if (isNewStudent(dashboard)
                && SynIntent.CAREER_QUESTION == decision.intent()
                && decision.paths().isEmpty()
                && !decision.tools().contains(SynToolIdentifier.SEARCH_KNOWLEDGE)) {
            decision =
                    agentPolicy.decide(
                            "GET_STARTED",
                            request.message(),
                            session.lastPaths(),
                            false,
                            request.resolvedResponseLanguage(),
                            memory.language());
        }
        String normalized =
                edu.hcmute.careersim.guidance.domain.KnowledgeChunker.normalize(request.message());
        if (pendingPlan != null
                && "FREE_TEXT".equals(request.resolvedActionType())
                && (normalized.contains("step") || normalized.contains("buoc"))
                && (normalized.contains("plan") || normalized.contains("ke hoach"))) {
            decision =
                    decision.withTools(
                            decision.tools().stream()
                                    .filter(t -> t != SynToolIdentifier.DRAFT_PLAN)
                                    .toList());
        }
        if (pendingPlan != null
                && !"FREE_TEXT".equals(request.resolvedActionType())
                && decision.intent() != SynIntent.SAFETY_BOUNDARY
                && (normalized.contains("step") || normalized.contains("buoc"))
                && (normalized.contains("plan")
                        || normalized.contains("ke hoach")
                        || normalized.contains("do"))) {
            int step =
                    normalized.matches(".*(second|thu hai|thu 2|buoc 2|step 2).*")
                            ? 1
                            : normalized.matches(".*(third|thu ba|thu 3|buoc 3|step 3).*")
                                    ? 2
                                    : normalized.matches(
                                                    ".*(first|dau tien|thu nhat|buoc 1|step 1).*")
                                            ? 0
                                            : -1;
            if (step >= 0 && step < pendingPlan.steps().size()) {
                var reply =
                        new SynMessageResponse(
                                UUID.randomUUID().toString(),
                                session.id(),
                                "DRAFT_PLAN",
                                decision.responseLanguage(),
                                memory.stage(),
                                ("VI".equals(decision.responseLanguage()) ? "Bước " : "Step ")
                                        + (step + 1)
                                        + ": "
                                        + pendingPlan.steps().get(step),
                                "STANDARD",
                                List.of("Read the saved session draft"),
                                List.of(),
                                null,
                                List.of(),
                                List.of(),
                                null,
                                List.of(),
                                List.of(),
                                pendingPlan);
                return new AgentRun(
                        reply,
                        "FALLBACK",
                        "READY",
                        null,
                        serialize(memory),
                        session.lastPaths(),
                        "PENDING_PLAN_CONTEXT");
            }
        }
        SynAgentToolbox.ToolResult tools;
        SynGuidanceProvider.AiReply aiReply = null;
        String provenance = "STANDARD";
        String auditSource = "FALLBACK";
        String auditStatus = "READY";
        String model = null;
        String reason = consentedToAi ? "STANDARD_POLICY" : "NO_AI_CONSENT";

        Optional<SynGuidanceProvider> provider =
                providers.stream().filter(SynGuidanceProvider::isAvailable).findFirst();
        boolean mayCallProvider =
                "FREE_TEXT".equals(request.resolvedActionType())
                        && consentedToAi
                        && provider.isPresent()
                        && SynIntent.SAFETY_BOUNDARY != decision.intent()
                        && SynIntent.CONFIRM_PLAN != decision.intent();
        if (mayCallProvider) {
            SynGuidanceProvider selected = provider.orElseThrow();
            try {
                if (selected.supportsToolCalling()) {
                    BoundedAgentResult result =
                            runBoundedToolCalling(
                                    selected,
                                    decision,
                                    request.message(),
                                    dashboard,
                                    requestedAttemptId,
                                    session,
                                    memory);
                    tools = result.tools();
                    aiReply = result.reply();
                } else {
                    tools =
                            toolbox.execute(
                                    decision,
                                    request.message(),
                                    dashboard,
                                    requestedAttemptId,
                                    session.summary());
                    var contextData =
                            new java.util.LinkedHashMap<String, Object>(tools.providerData());
                    if (pendingPlan != null) contextData.put("PENDING_PLAN", pendingPlan);
                    aiReply =
                            generateWithRepair(
                                    selected,
                                    request.message(),
                                    providerDashboard(
                                            dashboard,
                                            tools.selectedResults(),
                                            tools.evidenceReferences().stream()
                                                    .anyMatch(
                                                            r ->
                                                                    "ASSESSMENT"
                                                                                    .equals(
                                                                                            r
                                                                                                    .sourceType())
                                                                            || "SIMULATION"
                                                                                    .equals(
                                                                                            r
                                                                                                    .sourceType()))),
                                    providerContext(
                                                    decision,
                                                    session,
                                                    memory,
                                                    contextualObservations(
                                                            memory, tools.observations()))
                                            .withToolData(contextData),
                                    tools);
                }
                edu.hcmute.careersim.guidance.domain.SynReplyValidator.checkEvidence(
                        aiReply,
                        java.util.stream.Stream.concat(
                                        dashboard.interestSignals().stream()
                                                .map(DashboardResponse.InterestSignal::score),
                                        tools.selectedResults().stream()
                                                .map(DashboardResponse.RecentResult::score))
                                .toList());
                validateKnowledge(tools, aiReply);
                reason = "AI_READY";
                provenance = "AI";
                auditSource = "AI";
                model = selected.modelName();
            } catch (SynGuidanceProvider.ProviderException exception) {
                reason = exception.kind().name();
                aiReply = null;
                tools =
                        toolbox.execute(
                                decision,
                                request.message(),
                                dashboard,
                                requestedAttemptId,
                                session.summary());
                auditStatus = "REPLACED_BY_FALLBACK";
                log.warn("Syn provider fallback category={}", exception.kind());
            } catch (RuntimeException exception) {
                reason =
                        exception instanceof SynGuidanceProvider.ProviderException providerFailure
                                ? providerFailure.kind().name()
                                : "UNEXPECTED_PROVIDER_FAILURE";
                aiReply = null;
                tools =
                        toolbox.execute(
                                decision,
                                request.message(),
                                dashboard,
                                requestedAttemptId,
                                session.summary());
                auditStatus = "REPLACED_BY_FALLBACK";
                log.warn(
                        "Syn provider fallback category=UNEXPECTED_PROVIDER_FAILURE failure={}",
                        exception.getClass().getSimpleName());
            }
        } else {
            tools =
                    toolbox.execute(
                            decision,
                            request.message(),
                            dashboard,
                            requestedAttemptId,
                            session.summary());
        }

        String executionReason = reason;
        String text = aiReply == null ? tools.standardText() : aiReply.text();
        if (aiReply == null && !"NOT_REQUESTED".equals(tools.knowledge().status())) {
            boolean vi = "VI".equals(decision.responseLanguage());
            if (tools.knowledge().chunks().isEmpty() && requiresMissingKnowledge(tools)) {
                reason = tools.knowledge().status();
                text =
                        vi
                                ? "Mình chưa tìm thấy nguồn đã duyệt phù hợp để trả lời chắc chắn. Bạn có thể nói rõ khái niệm hoặc tình huống muốn tìm hiểu không?"
                                : "I could not find a suitable reviewed source for this question. Could you clarify the concept or situation you want to explore?";
            } else if (!tools.knowledge().chunks().isEmpty()) {
                var c = tools.knowledge().chunks().get(0);
                text =
                        (vi
                                        ? "Bạn có thể tham khảo đoạn tài liệu đã duyệt sau. Đây là kiến thức chung, không phải đánh giá về bạn.\n\n"
                                        : "Here is a reviewed passage. This is general knowledge, not an assessment of you.\n\n")
                                + c.content().substring(0, Math.min(c.content().length(), 1600))
                                + " ["
                                + c.referenceId()
                                + "]";
                reason = "REVIEWED_EXTRACT";
            }
        }
        SynExecutionScope.recordOutcome(executionReason, reason);
        List<String> suggestions =
                aiReply == null
                        ? tools.suggestions()
                        : aiReply.suggestions().stream().distinct().limit(2).toList();

        List<String> rememberedPaths =
                tools.paths().isEmpty() ? session.lastPaths() : tools.paths();
        MemoryState updatedMemory = updateMemory(memory, decision, tools);

        SynMessageResponse response =
                new SynMessageResponse(
                                UUID.randomUUID().toString(),
                                session.id(),
                                decision.intent().name(),
                                decision.responseLanguage(),
                                updatedMemory.stage(),
                                text,
                                provenance,
                                tools.activity(),
                                tools.evidenceReferences(),
                                tools.comparisonCard(),
                                tools.resourceCards(),
                                tools.simulationCards(),
                                tools.resultCard(),
                                tools.resultCards(),
                                suggestions,
                                decision.intent() == SynIntent.CONFIRM_PLAN
                                        ? synAgentDao.pendingPlan(studentId, session.id())
                                        : tools.planDraft())
                        .withWebSearch(tools.webSearch());
        return new AgentRun(
                response,
                auditSource,
                auditStatus,
                model,
                edu.hcmute.careersim.guidance.domain.SynMemoryEvidence.remember(
                        edu.hcmute.careersim.guidance.domain.SynResourceMemory.remember(
                                serialize(updatedMemory),
                                tools.resourceCards().isEmpty()
                                        ? edu.hcmute.careersim.guidance.domain.SynResourceMemory
                                                .read(session.summary())
                                        : tools.resourceCards().stream()
                                                .map(SynMessageResponse.ResourceCard::skill)
                                                .toList()),
                        tools.selectedResults().isEmpty()
                                ? edu.hcmute.careersim.guidance.domain.SynMemoryEvidence
                                        .selectedResult(session.summary())
                                : Long.valueOf(tools.selectedResults().get(0).id())),
                rememberedPaths,
                reason);
    }

    private BoundedAgentResult runBoundedToolCalling(
            SynGuidanceProvider provider,
            SynAgentPolicy.Decision deterministicDecision,
            String message,
            DashboardResponse dashboard,
            Long requestedAttemptId,
            SynAgentDao.SessionMemory session,
            MemoryState memory) {
        int completedPasses = advanceProviderPass(0);
        SynGuidanceProvider.ToolPlan toolPlan =
                provider.requestTools(
                        message,
                        providerContext(deterministicDecision, session, memory, List.of()),
                        List.of(SynToolIdentifier.values()));
        if (toolPlan.tools().isEmpty()
                || toolPlan.tools().size() > MAX_TOOLS_PER_PLAN
                || toolPlan.tools().stream().distinct().count() != toolPlan.tools().size()) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        }

        List<SynToolIdentifier> selected = new ArrayList<>(toolPlan.tools());
        for (var required : deterministicDecision.tools())
            if (!selected.contains(required)) selected.add(required);
        if (selected.size() > MAX_TOOLS_PER_PLAN)
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
        SynAgentPolicy.Decision selectedDecision = deterministicDecision.withTools(selected);
        SynAgentToolbox.ToolResult tools =
                toolbox.execute(
                        selectedDecision,
                        message,
                        dashboard,
                        requestedAttemptId,
                        session.summary());
        if (tools.degradedTools().containsAll(toolPlan.tools())) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.PROVIDER_REJECTED);
        }

        List<String> observations = contextualObservations(memory, tools.observations());
        completedPasses = advanceProviderPass(completedPasses);
        SynGuidanceProvider.AiReply reply =
                provider.synthesize(
                        message,
                        providerDashboard(
                                dashboard,
                                tools.selectedResults(),
                                tools.evidenceReferences().stream()
                                        .anyMatch(
                                                r ->
                                                        "ASSESSMENT".equals(r.sourceType())
                                                                || "SIMULATION"
                                                                        .equals(r.sourceType()))),
                        providerContext(deterministicDecision, session, memory, observations)
                                .withToolData(tools.providerData()),
                        toolPlan,
                        new SynGuidanceProvider.ToolExecution(
                                toolPlan.tools(),
                                tools.degradedTools(),
                                observations,
                                tools.providerData()));
        if (completedPasses != MAX_PROVIDER_PASSES) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
        }
        return new BoundedAgentResult(tools, reply);
    }

    private SynGuidanceProvider.AiReply generateWithRepair(
            SynGuidanceProvider provider,
            String message,
            DashboardResponse dashboard,
            SynGuidanceProvider.AgentContext context,
            SynAgentToolbox.ToolResult tools) {
        for (int pass = 0; pass < 2; pass++) {
            try {
                var reply = provider.generate(message, dashboard, context);
                edu.hcmute.careersim.guidance.domain.SynReplyValidator.checkEvidence(
                        reply,
                        java.util.stream.Stream.concat(
                                        dashboard.interestSignals().stream()
                                                .map(DashboardResponse.InterestSignal::score),
                                        tools.selectedResults().stream()
                                                .map(DashboardResponse.RecentResult::score))
                                .toList());
                validateKnowledge(tools, reply);
                return reply;
            } catch (SynGuidanceProvider.ProviderException failure) {
                if (pass != 0
                        || failure.kind() != SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT
                        || SynExecutionScope.remaining(java.time.Duration.ofSeconds(20)).toMillis()
                                < 4500) throw failure;
                SynExecutionScope.checkExternal();
                var data = new java.util.LinkedHashMap<String, Object>(context.toolData());
                data.put(
                        "VALIDATION_FAILURE",
                        failure.getCause() instanceof IllegalArgumentException
                                ? failure.getCause().getMessage()
                                : "SCHEMA_OR_CITATION");
                data.put(
                        "RESPONSE_REPAIR",
                        "The previous output failed local validation. Write a fresh answer using only this context: JSON text <=2400 characters; up to 3 distinct suggestions <=120 characters each. No raw URLs/HTML, invented citations or global competence claims. Explain each requested goal naturally. Use only supplied K/W references; if absent, label general knowledge and disclose unavailable search. Do not repeat or invent scores.");
                context = context.withToolData(data);
                SynExecutionScope.observe("OUTPUT_REPAIR", () -> true);
            }
        }
        throw new IllegalStateException("Bounded repair exhausted");
    }

    private boolean requiresMissingKnowledge(SynAgentToolbox.ToolResult tools) {
        return !"NOT_REQUESTED".equals(tools.knowledge().status())
                && tools.knowledge().chunks().isEmpty()
                && tools.resourceCards().isEmpty()
                && tools.webSearch().sources().isEmpty()
                && tools.planDraft() == null;
    }

    private void validateKnowledge(
            SynAgentToolbox.ToolResult tools, SynGuidanceProvider.AiReply reply) {
        var webMatcher = java.util.regex.Pattern.compile("\\[W[0-9]+\\]").matcher(reply.text());
        while (webMatcher.find()) {
            String reference = webMatcher.group().replace("[", "").replace("]", "");
            if (tools.webSearch().sources().stream()
                    .noneMatch(s -> s.referenceId().equals(reference)))
                throw new SynGuidanceProvider.ProviderException(
                        SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
        }
        var matcher = java.util.regex.Pattern.compile("\\[K(\\d+)\\]").matcher(reply.text());
        java.util.Set<Long> used = new java.util.HashSet<>();
        while (matcher.find()) {
            long id;
            try {
                id = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new SynGuidanceProvider.ProviderException(
                        SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
            }
            if (tools.knowledge().chunks().stream().noneMatch(c -> c.id() == id)
                    || knowledge == null
                    || knowledge.published(id).isEmpty())
                throw new SynGuidanceProvider.ProviderException(
                        SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
            used.add(id);
        }
        if (!tools.knowledge().chunks().isEmpty() && used.isEmpty())
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT);
    }

    private int advanceProviderPass(int completedPasses) {
        if (completedPasses >= MAX_PROVIDER_PASSES) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
        }
        return completedPasses + 1;
    }

    private SynGuidanceProvider.AgentContext providerContext(
            SynAgentPolicy.Decision decision,
            SynAgentDao.SessionMemory session,
            MemoryState memory,
            List<String> observations) {
        return new SynGuidanceProvider.AgentContext(
                decision.intent(),
                decision.responseLanguage(),
                memory.stage(),
                decision.responseDepth(),
                session.summary().replaceAll("\\|result=\\d+", ""),
                memory.coveredTopics().stream().map(SynIntent::name).toList(),
                observations);
    }

    public void persistMemory(long studentId, AgentRun run) {
        synAgentDao.updateSession(
                studentId,
                run.response().sessionId(),
                run.memorySummary(),
                run.response().intent(),
                run.memoryPaths());
    }

    public boolean clearSession(long studentId, UUID sessionId) {
        return synAgentDao.clearSession(studentId, sessionId);
    }

    private MemoryState readMemory(SynAgentDao.SessionMemory session, DashboardResponse dashboard) {
        String language = null;
        SynIntent goal = SynIntent.fromStored(session.lastIntent(), SynIntent.GENERAL);
        List<SynIntent> covered = new ArrayList<>();
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
                    case "goal" -> goal = SynIntent.fromStored(pair[1], goal);
                    case "covered" -> {
                        if (!pair[1].isBlank()) {
                            for (String topic : pair[1].split(",")) {
                                SynIntent parsed = SynIntent.fromStored(topic, null);
                                if (parsed != null) covered.add(parsed);
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
        LinkedHashSet<SynIntent> covered = new LinkedHashSet<>(current.coveredTopics());
        covered.add(decision.intent());
        List<SynIntent> boundedCovered =
                covered.stream().skip(Math.max(0, covered.size() - 6L)).toList();
        String nextAction =
                switch (decision.intent()) {
                    case GET_STARTED -> "CHOOSE_FIRST_EXPERIENCE";
                    case COMPARE_PATHS -> "TRY_COMPARISON_SIMULATION";
                    case LEARNING_RESOURCES -> "OPEN_ONE_REVIEWED_RESOURCE";
                    case IDENTIFY_GAPS -> "PRACTISE_ONE_EVIDENCE_GAP";
                    case DRAFT_PLAN -> "REVIEW_PLAN_DRAFT";
                    case REVIEW_LATEST -> "REFLECT_ON_RESULT";
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
                "FOLLOW_UP_" + decision.intent().name(),
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
                        + memory.currentGoal().name()
                        + "|covered="
                        + memory.coveredTopics().stream()
                                .map(SynIntent::name)
                                .collect(java.util.stream.Collectors.joining(","))
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

    private DashboardResponse providerDashboard(
            DashboardResponse dashboard,
            List<DashboardResponse.RecentResult> selectedResults,
            boolean personal) {
        List<DashboardResponse.SkillSummary> scopedSkills =
                selectedResults.isEmpty()
                        ? List.of()
                        : selectedResults.stream()
                                .flatMap(
                                        result ->
                                                result.outcomes().stream()
                                                        .map(
                                                                outcome ->
                                                                        new DashboardResponse
                                                                                .SkillSummary(
                                                                                outcome.label(),
                                                                                outcome.status(),
                                                                                result.title())))
                                .toList();
        return new DashboardResponse(
                dashboard.source(),
                dashboard.studentName(),
                dashboard.progress(),
                personal ? dashboard.interestSignals() : List.of(),
                scopedSkills,
                selectedResults,
                personal ? dashboard.plan() : null);
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
            SynIntent currentGoal,
            List<SynIntent> coveredTopics,
            String openQuestion,
            String proposedNextStep,
            int previousCompletedSteps,
            int currentCompletedSteps) {}

    private record BoundedAgentResult(
            SynAgentToolbox.ToolResult tools, SynGuidanceProvider.AiReply reply) {}

    public record AgentRun(
            SynMessageResponse response,
            String auditSource,
            String auditStatus,
            String modelName,
            String memorySummary,
            List<String> memoryPaths,
            String reason) {}
}
