package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.guidance.agent.service.AgentModelProvider.Decision;
import edu.hcmute.careersim.guidance.infrastructure.AgentTransportBudget;
import edu.hcmute.careersim.guidance.service.*;
import java.time.Duration;
import java.util.*;
import java.util.function.*;

/** Per execution, same-provider model attempts only. Never invokes or replays a tool. */
public final class AgentModelFailover {
    public static final int MAX_ATTEMPTS = 6;
    private final List<String> models;
    private final Consumer<Map<String, Object>> audit;
    private final LongConsumer sleep;
    private final IntSupplier jitter;
    private int index, attempts, repairs;
    private String fallbackReason = "PRIMARY";

    public AgentModelFailover(
            String primary, JsonNode experience, Consumer<Map<String, Object>> audit) {
        this(
                primary,
                experience,
                audit,
                millis -> {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw failure(SynGuidanceProvider.FailureKind.CONSENT_REVOKED);
                    }
                },
                () -> java.util.concurrent.ThreadLocalRandom.current().nextInt(51));
    }

    public AgentModelFailover(
            String primary,
            JsonNode experience,
            Consumer<Map<String, Object>> audit,
            LongConsumer sleep,
            IntSupplier jitter) {
        var chain = new ArrayList<String>();
        chain.add(primary);
        if (experience != null)
            experience.path("fallbackModelKeys").forEach(n -> chain.add(n.asText()));
        models = List.copyOf(chain);
        this.audit = audit;
        this.sleep = sleep;
        this.jitter = jitter;
    }

    public boolean buffered() {
        return models.size() > 1;
    }

    public Decision call(Function<String, Decision> dispatch) {
        return call(dispatch, () -> true);
    }

    public Decision call(Function<String, Decision> dispatch, BooleanSupplier safeToRetry) {
        int retry = 0;
        String reason = fallbackReason;
        while (attempts < MAX_ATTEMPTS) {
            SynExecutionScope.checkExternal();
            var left = SynExecutionScope.remaining(Duration.ofSeconds(60));
            // Leave time for a compatible fallback; do not stretch a short execution.
            var cap =
                    models.size() == 1
                            ? left
                            : index + 1 < models.size()
                                    ? Duration.ofMillis(
                                            Math.max(1, Math.min(12000, left.toMillis() * 2 / 5)))
                                    : Duration.ofMillis(Math.min(18000, left.toMillis()));
            String key = models.get(index);
            long started = System.nanoTime();
            attempts++;
            try {
                var result =
                        SynExecutionScope.observe(
                                "AGENT_MODEL",
                                () -> AgentTransportBudget.within(cap, () -> dispatch.apply(key)));
                record(key, reason, "SUCCEEDED", started);
                return result;
            } catch (SynGuidanceProvider.ProviderException failure) {
                record(key, failure.kind().name(), "FAILED", started);
                if (!safeToRetry.getAsBoolean()) throw failure;
                if (failure.kind() == SynGuidanceProvider.FailureKind.MALFORMED_OUTPUT
                        && repairInvalidOutput()) {
                    reason = "STRUCTURED_OUTPUT_INVALID";
                    retry = 0;
                    continue;
                }
                if (!retryable(failure)
                        && failure.kind()
                                != SynGuidanceProvider.FailureKind.PROVIDER_MODEL_UNAVAILABLE)
                    throw failure;
                reason = failure.kind().name();
                if (retryable(failure)
                        && failure.kind()
                                != SynGuidanceProvider.FailureKind.PROVIDER_TRANSPORT_DEADLINE
                        && retry == 0
                        && attempts < MAX_ATTEMPTS
                        && left.toMillis() > 1500) {
                    retry++;
                    pause(200L * (1L << (retry - 1)) + jitter.getAsInt());
                    continue;
                }
                if (index + 1 >= models.size()) throw failure;
                index++;
                retry = 0;
                fallbackReason = reason;
            }
        }
        throw failure(SynGuidanceProvider.FailureKind.ITERATION_LIMIT);
    }

    private void pause(long millis) {
        // Cancellation/consent is checked on both sides of the bounded backoff.
        SynExecutionScope.checkExternal();
        if (SynExecutionScope.remaining(Duration.ofSeconds(60)).toMillis() <= millis + 250)
            throw failure(SynGuidanceProvider.FailureKind.PROVIDER_TRANSPORT_DEADLINE);
        sleep.accept(millis);
        SynExecutionScope.checkExternal();
    }

    public boolean repairInvalidOutput() {
        if (repairs++ > 0 || attempts >= MAX_ATTEMPTS) return false;
        if (index + 1 < models.size()) index++;
        fallbackReason = "STRUCTURED_OUTPUT_INVALID";
        return true;
    }

    private boolean retryable(SynGuidanceProvider.ProviderException failure) {
        Object status = failure.diagnostics().get("providerHttpStatus");
        if (status instanceof Number n)
            return Set.of(408, 429, 500, 502, 503, 504).contains(n.intValue());
        if (failure.kind() == SynGuidanceProvider.FailureKind.PROVIDER_TRANSPORT_DEADLINE)
            return true;
        return failure.kind() == SynGuidanceProvider.FailureKind.PROVIDER_NETWORK_UNAVAILABLE
                && Set.of(
                                "CONNECTION_RESET",
                                "CONNECTION_FAILURE",
                                "TRANSPORT_TIMEOUT",
                                "BODY_TIMEOUT",
                                "TRANSPORT_DEADLINE")
                        .contains(failure.diagnostics().get("networkCategory"));
    }

    private void record(String effective, String reason, String status, long at) {
        audit.accept(
                Map.of(
                        "type",
                        "MODEL_ATTEMPT",
                        "requestedModel",
                        models.get(0),
                        "effectiveModel",
                        effective,
                        "attempt",
                        attempts,
                        "fallbackFrom",
                        index == 0 ? "" : models.get(index - 1),
                        "reason",
                        reason,
                        "status",
                        status,
                        "elapsedMs",
                        (System.nanoTime() - at) / 1000000));
    }

    private static SynGuidanceProvider.ProviderException failure(
            SynGuidanceProvider.FailureKind kind) {
        return new SynGuidanceProvider.ProviderException(kind);
    }
}
