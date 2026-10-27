package edu.hcmute.careersim.guidance.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class StudentAgentContracts {
    private StudentAgentContracts() {}

    public record CreateSession(UUID requestId, String title, List<UUID> contextSessionIds) {}

    public record Message(UUID requestId, String text) {}

    public record Session(
            String id,
            String agentId,
            String agentName,
            String configurationId,
            int configurationVersion,
            String title,
            String status,
            String summaryStatus,
            long nextSequence,
            long contextEpoch,
            long revision,
            Instant startedAt,
            String provider) {}

    public record Interaction(
            String id,
            long sequenceNumber,
            String actorType,
            JsonNode content,
            String executionId,
            Instant createdAt,
            boolean unavailable) {}

    public record Event(long sequenceNumber, String type, JsonNode payload) {}

    public record Run(
            String id,
            String sessionId,
            String taskType,
            String status,
            String errorCode,
            JsonNode output,
            JsonNode toolStepsAndSources,
            String provenance,
            String configurationId,
            String configurationHash,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCost,
            String costStatus,
            Long latencyMs,
            boolean cancellationRequested,
            long lastEventSequence,
            String reportId,
            String planContentHash) {}

    public record Report(
            String id,
            String sourceExecutionId,
            String schemaVersion,
            JsonNode structuredInsights,
            JsonNode evidenceRefs,
            Instant createdAt) {}

    public record ConfirmSuggestion(
            UUID sourceExecutionId,
            UUID requestId,
            String contentHash,
            String expectedActivePlanId,
            Long expectedRevision) {}
}
