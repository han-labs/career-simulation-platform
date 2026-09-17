package edu.hcmute.careersim.guidance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SynMessageRequest(
        @NotBlank(message = "Message is required.")
                @Size(max = 600, message = "Message must not exceed 600 characters.")
                String message,
        @Pattern(
                        regexp =
                                "FREE_TEXT|GET_STARTED|EXPLAIN_RIASEC|REVIEW_LATEST|NEEDS_EVIDENCE|EXPLORE_NEXT|DRAFT_PLAN|COMPARE_PATHS|LEARNING_RESOURCES|CAREER_QUESTION",
                        message = "Action type is not supported.")
                String actionType,
        UUID sessionId,
        @Pattern(regexp = "AUTO|EN|VI", message = "Response language is not supported.")
                String responseLanguage,
        @Valid Context context) {

    public SynMessageRequest(String message, String actionType, Context context) {
        this(message, actionType, null, "AUTO", context);
    }

    public SynMessageRequest(String message, String actionType, UUID sessionId, Context context) {
        this(message, actionType, sessionId, "AUTO", context);
    }

    public String resolvedActionType() {
        return actionType == null || actionType.isBlank() ? "FREE_TEXT" : actionType;
    }

    public String resolvedResponseLanguage() {
        return responseLanguage == null || responseLanguage.isBlank() ? "AUTO" : responseLanguage;
    }

    public record Context(Long attemptId) {}
}
