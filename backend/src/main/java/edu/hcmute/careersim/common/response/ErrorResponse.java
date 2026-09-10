package edu.hcmute.careersim.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors,
        Instant timestamp) {

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(false, code, message, path, List.of(), Instant.now());
    }

    public static ErrorResponse validation(
            String code, String message, String path, List<FieldViolation> fieldErrors) {
        return new ErrorResponse(false, code, message, path, fieldErrors, Instant.now());
    }

    public record FieldViolation(String field, String message) {}
}
