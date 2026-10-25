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

    public record Rubric(int version, String evaluatorKey, JsonNode quizScoringConfig) {}

    public record TaskProgress(
            String taskId, boolean accessible, boolean finalized, boolean reviewed) {}

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
            Answer modelAnswer,
            String taskType,
            Rubric rubric) {
        public Task(
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
                Answer modelAnswer) {
            this(
                    id,
                    position,
                    title,
                    workplaceContext,
                    instructions,
                    required,
                    translations,
                    definitions,
                    resources,
                    skillIds,
                    modelAnswer,
                    "PRACTICAL",
                    null);
        }
    }

    public record Draft(
            @Valid @NotNull Metadata metadata, @NotNull @Size(max = 100) List<Task> tasks) {}

    public record Create(@NotBlank String slug, @Valid @NotNull Draft draft) {}

    public record Clone(String sourceVersionId) {}

    public record Availability(@NotBlank String status) {}

    public record ClassificationRequest(
            @Pattern(regexp = "IT") String industryCode) {}

    public record Classification(
            String scenarioId, String industryCode, long revision) {}

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

    public record CatalogQuery(
            String q,
            String careerTrack,
            String difficulty,
            String sort,
            String cursor,
            int limit,
            String industry) {
        public CatalogQuery(
                String q,
                String careerTrack,
                String difficulty,
                String sort,
                String cursor,
                int limit) {
            this(q, careerTrack, difficulty, sort, cursor, limit, null);
        }
    }

    public record CatalogPage(List<Summary> items, String nextCursor) {}

    public record PublicMedia(
            String cardImageUrl,
            String heroImageUrl,
            String companyLogoUrl,
            String overviewVideoUrl) {}

    public record TaskOutline(
            String id,
            int position,
            String title,
            String description,
            boolean required,
            String taskType) {}

    public record ParticipationSummary(
            String scenarioVersionId,
            String scenarioId,
            String title,
            int versionNumber,
            long enrollments,
            long participants,
            long completions) {}

    public record Summary(
            String id,
            String slug,
            String scenarioVersionId,
            String enterpriseId,
            String enterpriseName,
            String industryCode,
            Metadata metadata,
            PublicMedia media,
            List<TaskOutline> taskOutline,
            List<String> skills) {
        public Summary(
                String id,
                String slug,
                String scenarioVersionId,
                String enterpriseId,
                String enterpriseName,
                Metadata metadata) {
            this(
                    id,
                    slug,
                    scenarioVersionId,
                    enterpriseId,
                    enterpriseName,
                    null,
                    metadata,
                    null,
                    List.of(),
                    List.of());
        }

        public Summary(
                String id,
                String slug,
                String scenarioVersionId,
                String enterpriseId,
                String enterpriseName,
                String industryCode,
                Metadata metadata) {
            this(
                    id,
                    slug,
                    scenarioVersionId,
                    enterpriseId,
                    enterpriseName,
                    industryCode,
                    metadata,
                    null,
                    List.of(),
                    List.of());
        }

        public Summary(
                String id,
                String slug,
                String scenarioVersionId,
                String enterpriseId,
                String enterpriseName,
                String industryCode,
                Metadata metadata,
                PublicMedia media) {
            this(
                    id,
                    slug,
                    scenarioVersionId,
                    enterpriseId,
                    enterpriseName,
                    industryCode,
                    metadata,
                    media,
                    List.of(),
                    List.of());
        }
    }

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
            List<Item> items,
            JsonNode quizResult,
            Instant reviewedAt,
            String submittedByUserId) {
        public Submission(
                String submissionId,
                String enrollmentId,
                String taskId,
                String scenarioVersionId,
                String status,
                long revision,
                Instant submittedAt,
                String completionId,
                List<Item> items) {
            this(
                    submissionId,
                    enrollmentId,
                    taskId,
                    scenarioVersionId,
                    status,
                    revision,
                    submittedAt,
                    completionId,
                    items,
                    null,
                    null,
                    null);
        }
    }

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
            int finalRequiredTasks,
            String progressionMode,
            List<TaskProgress> progression) {}

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
