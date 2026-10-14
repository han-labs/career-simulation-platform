package edu.hcmute.careersim.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sign in is required."),
    DOMAIN_VALIDATION(
            HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_VALIDATION", "The supplied value is invalid."),
    PRECONDITION_REQUIRED(
            HttpStatus.PRECONDITION_REQUIRED,
            "PRECONDITION_REQUIRED",
            "If-Match revision is required."),
    WORKFLOW_UPGRADE_REQUIRED(
            HttpStatus.CONFLICT,
            "WORKFLOW_UPGRADE_REQUIRED",
            "Use the authenticated /api/v2 workflow; your historical work is preserved."),
    FINAL_SUBMISSION_LOCKED(
            HttpStatus.CONFLICT, "FINAL_SUBMISSION_LOCKED", "Final submissions cannot be changed."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with the current state."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Please try again later."),
    SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SERVICE_UNAVAILABLE",
            "The service is temporarily unavailable."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request is invalid."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to perform this action."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "One or more fields are invalid."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "The request could not be completed. Please try again later.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;
}
