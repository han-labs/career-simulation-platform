package edu.hcmute.careersim.assessment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class VersionedAssessmentContracts {
    private VersionedAssessmentContracts() {}

    public record TypeRequest(
            @NotBlank @Size(max = 80) String code, @NotBlank @Size(max = 160) String name) {}

    public record QuestionDraft(
            @Positive int position,
            @NotBlank String prompt,
            @NotBlank String responseType,
            JsonNode responseConfig,
            JsonNode translations,
            boolean required) {}

    public record VersionDraft(
            @NotBlank String evaluatorKey,
            @NotBlank String evaluatorVersion,
            JsonNode scoringConfig,
            @NotEmpty @Size(max = 200) @Valid List<QuestionDraft> questions) {}

    public record Start(@NotNull UUID requestId) {}

    public record Answer(@NotBlank String questionId, JsonNode answerData) {}

    public record Responses(@NotNull @Size(max = 200) @Valid List<Answer> items) {}

    public record Question(
            String id,
            int position,
            String prompt,
            String responseType,
            JsonNode responseConfig,
            JsonNode translations,
            boolean required) {}

    public record Type(String id, String code, String name, String activeVersionId) {}

    public record Version(
            String id,
            String typeId,
            int versionNumber,
            String status,
            String evaluatorKey,
            String evaluatorVersion,
            JsonNode scoringConfig,
            String hash,
            long revision,
            List<Question> questions) {}

    public record Attempt(
            String id,
            String assessmentVersionId,
            String status,
            long revision,
            Instant startedAt,
            Instant submittedAt,
            List<Question> questions,
            List<Answer> responses) {}

    public record Result(
            String attemptId,
            String assessmentTypeCode,
            String assessmentVersionId,
            String evaluatorKey,
            String evaluatorVersion,
            JsonNode resultData,
            Instant calculatedAt) {}
}
