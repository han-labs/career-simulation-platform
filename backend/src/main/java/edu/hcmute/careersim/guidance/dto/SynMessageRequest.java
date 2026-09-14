package edu.hcmute.careersim.guidance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SynMessageRequest(
        @NotBlank(message = "Message is required.")
                @Size(max = 600, message = "Message must not exceed 600 characters.")
                String message,
        @Pattern(
                        regexp =
                                "FREE_TEXT|EXPLAIN_RIASEC|REVIEW_LATEST|NEEDS_EVIDENCE|EXPLORE_NEXT|DRAFT_PLAN",
                        message = "Action type is not supported.")
                String actionType,
        @Valid Context context) {

    public String resolvedActionType() {
        return actionType == null || actionType.isBlank() ? "FREE_TEXT" : actionType;
    }

    public record Context(Long attemptId) {}
}
