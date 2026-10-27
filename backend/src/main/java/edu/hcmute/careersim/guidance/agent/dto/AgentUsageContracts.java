package edu.hcmute.careersim.guidance.agent.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Explicit operational allowlist. No execution inputs, outputs, traces or Student identity. */
public final class AgentUsageContracts {
    private AgentUsageContracts() {}

    public record Filter(
            Instant from,
            Instant to,
            UUID agent,
            String provider,
            String model,
            String outcome,
            String mode,
            int limit,
            String cursor) {}

    public record Position(Instant startedAt, UUID id) {}

    public record Summary(
            long requests,
            long succeeded,
            long failed,
            long refused,
            long cancelled,
            long pending,
            BigDecimal recordedEstimatedCostUsd,
            long estimatedCostExecutions,
            long partialCostExecutions,
            long unknownCostExecutions,
            long notApplicableCostExecutions,
            BigDecimal averageLatencyMs,
            long latencySamples,
            long missingLatencyExecutions) {}

    public record Bucket(
            Instant startedAt,
            long requests,
            long succeeded,
            long failed,
            long refused,
            long cancelled,
            long pending) {}

    public record Execution(
            String id,
            Instant startedAt,
            Instant finishedAt,
            String mode,
            String agentId,
            String agentName,
            String configurationId,
            Integer configurationVersion,
            String promptHash,
            String provider,
            String model,
            String outcome,
            String failureCategory,
            Long latencyMs,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCostUsd,
            String costStatus,
            long nativeToolCount,
            List<String> tools,
            long serverRetrievalCount,
            Long modelAttempts,
            String effectiveModel,
            List<String> fallbackReasons) {}

    public record AgentOption(String id, String name) {}

    public record ModelOption(String provider, String model) {}

    public record Options(List<AgentOption> agents, List<ModelOption> models, boolean truncated) {}

    public record Dashboard(
            Instant from,
            Instant to,
            String bucketUnit,
            Summary summary,
            List<Bucket> trend,
            Options options,
            List<Execution> executions,
            String nextCursor) {}
}
