package edu.hcmute.careersim.simulation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class ScenarioContracts {
    private ScenarioContracts() {}

    public record Metadata(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 120) String targetRole,
            @NotBlank @Size(max = 60) String careerTrack,
            @NotBlank @Size(max = 600) String summary,
            @NotBlank String introduction,
            @NotBlank String learningObjectives,
            @Pattern(regexp = "INTRODUCTORY|INTERMEDIATE|ADVANCED") String difficulty,
            @Positive int estimatedMinutes,
            JsonNode translations,
            JsonNode provenance) {}

    public record Definition(
            String id,
            int position,
            String name,
            String format,
            boolean required,
            JsonNode validationConfig) {}

    public record Resource(
            String id,
            int position,
            String title,
            String fileAssetId,
            String externalUrl,
            String releasePolicy) {}

    public record Answer(String content, JsonNode translations) {}

    public record Task(
            String id,
            int position,
            String title,
            String workplaceContext,
            String instructions,
            boolean required,
            JsonNode translations,
            List<Definition> definitions,
            List<Resource> resources,
            List<String> skillIds,
            Answer modelAnswer) {}

    public record Draft(
            @Valid @NotNull Metadata metadata, @NotNull @Size(max = 100) List<Task> tasks) {}

    public record Create(@NotBlank String slug, @Valid @NotNull Draft draft) {}

    public record Clone(String sourceVersionId) {}

    public record Availability(@NotBlank String status) {}

    public record Version(
            String id,
            String scenarioId,
            String enterpriseId,
            int versionNumber,
            String status,
            String availabilityStatus,
            long revision,
            long scenarioRevision,
            String contentHash,
            Metadata metadata,
            List<Task> tasks) {}

    public record Summary(
            String id,
            String slug,
            String scenarioVersionId,
            String enterpriseId,
            String enterpriseName,
            Metadata metadata) {}

    public record SkillRequest(@NotBlank String code, @NotBlank String name) {}

    public record Skill(String id, String code, String name, String status, long revision) {}

    public record Item(
            String definitionId,
            String contentText,
            String contentUrl,
            String fileAssetId,
            JsonNode contentJson) {
        public Item {
            if (contentJson != null && contentJson.isNull()) contentJson = null;
        }
    }

    public record Items(@NotNull @Size(max = 100) List<Item> items) {}

    public record Finalize(@NotNull UUID requestId, @NotNull @Size(max = 100) List<Item> items) {}

    public record Submission(
            String submissionId,
            String enrollmentId,
            String taskId,
            String scenarioVersionId,
            String status,
            long revision,
            Instant submittedAt,
            String completionId,
            List<Item> items) {}

    public record Enrollment(
            String id,
            String scenarioVersionId,
            String status,
            long revision,
            Instant enrolledAt,
            Instant startedAt,
            Instant completedAt,
            String completionId,
            Metadata metadata,
            List<Task> tasks,
            int requiredTasks,
            int finalRequiredTasks) {}

    public record Completion(
            String id,
            String enrollmentId,
            Instant completedAt,
            String resumeSnippetText,
            String snippetTemplateVersion,
            String certificateCode,
            JsonNode certificateSnapshot,
            Instant certificateIssuedAt) {}

    public record Participation(
            String submissionId,
            String taskId,
            String scenarioVersionId,
            String skillCode,
            String skillNameSnapshot,
            Instant submittedAt) {}
}
