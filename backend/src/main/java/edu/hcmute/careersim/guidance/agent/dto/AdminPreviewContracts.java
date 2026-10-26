package edu.hcmute.careersim.guidance.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;

public final class AdminPreviewContracts {
    private AdminPreviewContracts() {}

    public record Conversation(
            String id,
            String agentId,
            String configurationId,
            String status,
            long epoch,
            long revision,
            Instant createdAt,
            Instant updatedAt) {}

    public record NewChat(UUID requestId, UUID configurationId) {}

    public record Message(UUID requestId, String query) {}

    public record Change(boolean confirmed) {}

    public record Turn(
            long sequence, AgentContracts.Execution execution, boolean sourceUnavailable) {}

    public record Transcript(
            Conversation conversation,
            int configurationVersion,
            String agentName,
            String greeting,
            JsonNode chat,
            boolean canSend,
            String unavailableReason,
            List<Turn> turns,
            Long nextBefore) {}

    public record Page(List<Conversation> items, Integer nextOffset) {}

    public record Scope(UUID conversation, long epoch, String history) {}
}
