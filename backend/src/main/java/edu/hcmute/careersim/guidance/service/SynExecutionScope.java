package edu.hcmute.careersim.guidance.service;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Request-scoped deadline and consent revalidation; always closed by the use-case service. */
public final class SynExecutionScope implements AutoCloseable {
    private static final ThreadLocal<SynExecutionScope> CURRENT = new ThreadLocal<>();
    private final java.util.List<java.util.Map<String, Object>> calls = new java.util.ArrayList<>();
    private final java.util.List<java.util.Map<String, Object>> usage = new java.util.ArrayList<>();
    private final java.util.List<java.util.Map<String, Object>> toolRuns =
            new java.util.ArrayList<>();
    private final long deadline;
    private final java.util.Map<String, KnowledgeRetrievalService.Retrieval> retrievals =
            new java.util.HashMap<>();
    private final java.util.Map<String, WebSearchProvider.Result> searches =
            new java.util.HashMap<>();

    public static WebSearchProvider.Result cachedSearch(String query) {
        var scope = CURRENT.get();
        return scope == null ? null : scope.searches.get(query);
    }

    public static void cacheSearch(String query, WebSearchProvider.Result result) {
        var scope = CURRENT.get();
        if (scope != null) scope.searches.put(query, result);
    }

    private final BooleanSupplier externalAllowed;
    private final SynExecutionScope previous;
    private java.util.Map<String, String> outcome = java.util.Map.of();
    private String phase = "UNSCOPED";

    public static void recordOutcome(String executionReason, String responseMode) {
        var scope = CURRENT.get();
        if (scope != null)
            scope.outcome =
                    java.util.Map.of(
                            "executionReason", executionReason, "responseMode", responseMode);
    }

    private SynExecutionScope(Duration budget, BooleanSupplier externalAllowed) {
        this.deadline = System.nanoTime() + budget.toNanos();
        this.externalAllowed = externalAllowed;
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static SynExecutionScope open(Duration budget, BooleanSupplier externalAllowed) {
        return new SynExecutionScope(budget, externalAllowed);
    }

    public static <T> T observe(String phase, java.util.function.Supplier<T> action) {
        long start = System.nanoTime();
        var current = CURRENT.get();
        String previousPhase = current == null ? "UNSCOPED" : current.phase;
        if (current != null) current.phase = phase;
        boolean succeeded = false;
        try {
            T value = action.get();
            succeeded = true;
            return value;
        } finally {
            var scope = CURRENT.get();
            if (scope != null)
                scope.calls.add(
                        java.util.Map.of(
                                "phase",
                                phase,
                                "elapsedMs",
                                (System.nanoTime() - start) / 1_000_000,
                                "succeeded",
                                succeeded));
            if (current != null) current.phase = previousPhase;
        }
    }

    public static void recordTools(
            java.util.List<edu.hcmute.careersim.guidance.domain.SynToolIdentifier> requested,
            java.util.List<edu.hcmute.careersim.guidance.domain.SynToolIdentifier> degraded,
            KnowledgeRetrievalService.Retrieval retrieval) {
        var scope = CURRENT.get();
        if (scope != null)
            scope.toolRuns.add(
                    java.util.Map.of(
                            "requested",
                            requested,
                            "degraded",
                            degraded,
                            "retrievalStatus",
                            retrieval.status(),
                            "knowledgeVersions",
                            retrieval.chunks().stream()
                                    .map(c -> c.versionId())
                                    .distinct()
                                    .toList()));
    }

    public static void recordUsage(String provider, String model, Number input, Number output) {
        var scope = CURRENT.get();
        if (scope != null) {
            var row = new java.util.LinkedHashMap<String, Object>();
            row.put("provider", provider);
            row.put("model", model);
            row.put("phase", scope.phase);
            row.put("inputTokens", validTokens(input));
            row.put("outputTokens", validTokens(output));
            row.put(
                    "status",
                    validTokens(input) == null || validTokens(output) == null
                            ? "NOT_REPORTED"
                            : "REPORTED");
            scope.usage.add(row);
        }
    }

    public static Long validTokens(Number value) {
        if (value == null) return null;
        try {
            long result = new java.math.BigDecimal(value.toString()).longValueExact();
            return result >= 0 && result <= 1_000_000_000_000L ? result : null;
        } catch (ArithmeticException | NumberFormatException invalid) {
            return null;
        }
    }

    public static Long outputWithThinking(Number visible, Number thinking) {
        Long output = validTokens(visible);
        Long thoughts = thinking == null ? Long.valueOf(0) : validTokens(thinking);
        return output == null || thoughts == null ? null : validTokens(output + thoughts);
    }

    public static java.util.Map<String, Object> trace() {
        var scope = CURRENT.get();
        return java.util.Map.of(
                "calls",
                scope == null ? java.util.List.of() : java.util.List.copyOf(scope.calls),
                "tokenUsage",
                scope == null || scope.usage.isEmpty()
                        ? "NOT_REPORTED"
                        : java.util.List.copyOf(scope.usage),
                "cost",
                "NOT_ESTIMATED",
                "outcome",
                scope == null ? java.util.Map.of() : scope.outcome,
                "toolRuns",
                scope == null ? java.util.List.of() : java.util.List.copyOf(scope.toolRuns));
    }

    public static KnowledgeRetrievalService.Retrieval cachedRetrieval(String query) {
        var scope = CURRENT.get();
        return scope == null ? null : scope.retrievals.get(query);
    }

    public static void cacheRetrieval(String query, KnowledgeRetrievalService.Retrieval result) {
        var scope = CURRENT.get();
        if (scope != null) scope.retrievals.put(query, result);
    }

    public static boolean mayUseExternal() {
        SynExecutionScope scope = CURRENT.get();
        return scope != null && scope.externalAllowed.getAsBoolean();
    }

    public static Duration remaining(Duration maximum) {
        SynExecutionScope scope = CURRENT.get();
        if (scope == null) return maximum;
        long available = scope.deadline - System.nanoTime() - Duration.ofSeconds(2).toNanos();
        if (available <= 0 || Thread.currentThread().isInterrupted()) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.TIMEOUT_OR_TRANSPORT);
        }
        return Duration.ofNanos(Math.min(available, maximum.toNanos()));
    }

    public static void checkExternal() {
        SynExecutionScope scope = CURRENT.get();
        if (scope != null && !scope.externalAllowed.getAsBoolean()) {
            throw new SynGuidanceProvider.ProviderException(
                    SynGuidanceProvider.FailureKind.CONSENT_REVOKED);
        }
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
}
