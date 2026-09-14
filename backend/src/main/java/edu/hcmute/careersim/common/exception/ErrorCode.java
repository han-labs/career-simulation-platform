package edu.hcmute.careersim.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
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
