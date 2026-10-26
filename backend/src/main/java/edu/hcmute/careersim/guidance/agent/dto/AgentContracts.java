package edu.hcmute.careersim.guidance.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class AgentContracts {
    private AgentContracts() {}

    public record Agent(
            String id,
            String name,
            String purpose,
            String status,
            long revision,
            String publishedConfigurationId,
            Instant createdAt) {}

    public record AgentInput(String name, String purpose, String status) {}

    public record Configuration(
            String id,
            String agentId,
            int versionNumber,
            String modelKey,
            String promptTemplate,
            String promptTemplateRef,
            String promptHash,
            JsonNode toolPolicy,
            JsonNode memoryPolicy,
            JsonNode outputPolicy,
            List<String> knowledgeBaseIds,
            String status,
            String contentHash,
            Instant createdAt,
            Instant publishedAt,
            JsonNode experienceConfig) {}

    public record ConfigurationInput(
            String modelKey,
            String promptTemplate,
            String promptTemplateRef,
            JsonNode toolPolicy,
            JsonNode memoryPolicy,
            JsonNode outputPolicy,
            List<UUID> knowledgeBaseIds,
            JsonNode experienceConfig) {
        public ConfigurationInput(
                String modelKey,
                String promptTemplate,
                String promptTemplateRef,
                JsonNode toolPolicy,
                JsonNode memoryPolicy,
                JsonNode outputPolicy,
                List<UUID> knowledgeBaseIds) {
            this(
                    modelKey,
                    promptTemplate,
                    promptTemplateRef,
                    toolPolicy,
                    memoryPolicy,
                    outputPolicy,
                    knowledgeBaseIds,
                    null);
        }
    }

    public record Preview(UUID requestId, String query) {}

    public record Assign(String studentUserId) {}

    public record RoleBinding(
            String role, String agentId, String agentName, long revision, Instant updatedAt) {}

    public record RoleActivation(
            List<String> roles, Map<String, Long> expectedRoleRevisions, boolean confirmed) {}

    public record Assignment(
            String id,
            String agentId,
            String studentUserId,
            String assignedByAdminUserId,
            String status,
            Instant assignedAt,
            Instant revokedAt,
            String studentDisplayName) {}

    public record EligibleStudents(
            List<edu.hcmute.careersim.identity.access.ActorAccess.StudentIdentity> items,
            String nextAfterId) {}

    public record PublicationReadiness(
            String configurationId,
            String configurationHash,
            long agentRevision,
            String configurationStatus,
            boolean agentEditable,
            boolean knowledgeAvailable,
            boolean previewSucceeded,
            boolean publishable) {}

    public record PublicAgent(String id, String name, String purpose, boolean runtimeAvailable) {
        public PublicAgent(String id, String name, String purpose) {
            this(id, name, purpose, true);
        }
    }

    public record Execution(
            String id,
            String executionMode,
            String taskType,
            String initiatedByUserId,
            String sessionId,
            String configurationId,
            String inputInteractionId,
            String knowledgeBaseId,
            UUID requestId,
            JsonNode inputSnapshot,
            String status,
            String errorCode,
            JsonNode toolStepsAndSources,
            JsonNode outputSnapshot,
            String modelKey,
            String promptHash,
            String configurationHash,
            Long inputTokens,
            Long outputTokens,
            BigDecimal estimatedCost,
            String costStatus,
            Long latencyMs,
            JsonNode pricingSnapshot,
            Instant startedAt,
            Instant finishedAt,
            boolean cancellationRequested) {}
}
