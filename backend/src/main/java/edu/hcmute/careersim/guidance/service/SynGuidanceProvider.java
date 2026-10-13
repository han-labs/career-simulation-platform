package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.domain.SynIntent;
import edu.hcmute.careersim.guidance.domain.SynToolIdentifier;
import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import java.util.List;

/** Provider-neutral boundary for optional, evidence-grounded Syn guidance. */
public interface SynGuidanceProvider {

    boolean isAvailable();

    String modelName();

    AiReply generate(String message, DashboardResponse dashboard, AgentContext agentContext);

    default boolean supportsToolCalling() {
        return false;
    }

    default ToolPlan requestTools(
            String message, AgentContext agentContext, List<SynToolIdentifier> availableTools) {
        throw new ProviderException(FailureKind.PROVIDER_REJECTED);
    }

    default AiReply synthesize(
            String message,
            DashboardResponse dashboard,
            AgentContext agentContext,
            ToolPlan toolPlan,
            ToolExecution toolExecution) {
        throw new ProviderException(FailureKind.PROVIDER_REJECTED);
    }

    default AiReply generate(String message, DashboardResponse dashboard) {
        return generate(
                message,
                dashboard,
                new AgentContext(
                        SynIntent.CAREER_QUESTION,
                        "EN",
                        "EVIDENCE_READY",
                        "STANDARD",
                        "",
                        List.of(),
                        List.of()));
    }

    record AiReply(String text, List<String> suggestions) {}

    /**
     * Validated tool selection plus optional opaque provider state needed to continue the same
     * bounded exchange. The continuation is request-scoped and must never be logged or persisted.
     */
    record ToolPlan(List<SynToolIdentifier> tools, String providerContinuation) {

        public ToolPlan(List<SynToolIdentifier> tools) {
            this(tools, null);
        }

        public ToolPlan {
            tools = List.copyOf(tools);
            providerContinuation =
                    providerContinuation == null || providerContinuation.isBlank()
                            ? null
                            : providerContinuation;
        }
    }

    record ToolExecution(
            List<SynToolIdentifier> requestedTools,
            List<SynToolIdentifier> degradedTools,
            List<String> observations,
            java.util.Map<String, Object> toolData) {
        public ToolExecution(
                List<SynToolIdentifier> requested,
                List<SynToolIdentifier> degraded,
                List<String> observations) {
            this(requested, degraded, observations, java.util.Map.of());
        }

        public ToolExecution {
            requestedTools = List.copyOf(requestedTools);
            degradedTools = List.copyOf(degradedTools);
            observations = List.copyOf(observations);
        }
    }

    record AgentContext(
            SynIntent intent,
            String responseLanguage,
            String explorationStage,
            String responseDepth,
            String sessionSummary,
            List<String> coveredTopics,
            List<String> observations,
            java.util.Map<String, Object> toolData) {
        public AgentContext(
                SynIntent intent,
                String language,
                String stage,
                String depth,
                String summary,
                List<String> topics,
                List<String> observations) {
            this(intent, language, stage, depth, summary, topics, observations, java.util.Map.of());
        }

        public AgentContext withToolData(java.util.Map<String, Object> data) {
            return new AgentContext(
                    intent,
                    responseLanguage,
                    explorationStage,
                    responseDepth,
                    sessionSummary,
                    coveredTopics,
                    observations,
                    data);
        }

        public AgentContext(SynIntent intent, String sessionSummary, List<String> observations) {
            this(
                    intent,
                    "EN",
                    "EVIDENCE_READY",
                    "STANDARD",
                    sessionSummary,
                    List.of(),
                    observations);
        }
    }

    enum FailureKind {
        TIMEOUT_OR_TRANSPORT,
        PROVIDER_REJECTED,
        PROVIDER_RATE_LIMITED,
        PROVIDER_UNAVAILABLE,
        MALFORMED_OUTPUT,
        UNAVAILABLE_TOOL,
        EMPTY_EVIDENCE,
        CONSENT_REVOKED,
        ITERATION_LIMIT
    }

    final class ProviderException extends RuntimeException {
        private final FailureKind kind;

        public ProviderException(FailureKind kind, Throwable cause) {
            super(kind.name(), cause);
            this.kind = kind;
        }

        public ProviderException(FailureKind kind) {
            super(kind.name());
            this.kind = kind;
        }

        public FailureKind kind() {
            return kind;
        }
    }
}
