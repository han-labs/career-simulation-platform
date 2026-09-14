package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dto.DashboardResponse;
import java.util.List;

/** Provider-neutral boundary for optional, evidence-grounded Syn guidance. */
public interface SynGuidanceProvider {

    boolean isAvailable();

    String modelName();

    AiReply generate(String message, DashboardResponse dashboard);

    record AiReply(String text, List<String> suggestions) {}

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
