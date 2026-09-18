package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import java.util.List;

/** Provider-neutral boundary for optional, evidence-grounded Syn guidance. */
public interface SynGuidanceProvider {

    boolean isAvailable();

    String modelName();

    AiReply generate(String message, DashboardResponse dashboard, AgentContext agentContext);

    default AiReply generate(String message, DashboardResponse dashboard) {
        return generate(
                message,
                dashboard,
                new AgentContext(
                        "CAREER_QUESTION",
                        "EN",
                        "EVIDENCE_READY",
                        "STANDARD",
                        "",
                        List.of(),
                        List.of()));
    }

    record AiReply(String text, List<String> suggestions) {}

    record AgentContext(
            String intent,
            String responseLanguage,
            String explorationStage,
            String responseDepth,
            String sessionSummary,
            List<String> coveredTopics,
            List<String> observations) {

        public AgentContext(String intent, String sessionSummary, List<String> observations) {
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
        MALFORMED_OUTPUT
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
