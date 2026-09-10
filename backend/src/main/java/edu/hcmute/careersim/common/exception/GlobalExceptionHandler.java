package edu.hcmute.careersim.common.exception;

import edu.hcmute.careersim.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.getCode(), exception.getMessage(), request.getRequestURI());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> violations =
                exception.getBindingResult().getFieldErrors().stream()
                        .sorted(Comparator.comparing(FieldError::getField))
                        .map(
                                error ->
                                        new ErrorResponse.FieldViolation(
                                                error.getField(), error.getDefaultMessage()))
                        .toList();
        return validationResponse(request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> violations =
                exception.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new ErrorResponse.FieldViolation(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()))
                        .sorted(Comparator.comparing(ErrorResponse.FieldViolation::field))
                        .toList();
        return validationResponse(request, violations);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(
                        ErrorResponse.of(
                                errorCode.getCode(),
                                errorCode.getDefaultMessage(),
                                request.getRequestURI()));
    }

    private ResponseEntity<ErrorResponse> validationResponse(
            HttpServletRequest request, List<ErrorResponse.FieldViolation> violations) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(
                        ErrorResponse.validation(
                                errorCode.getCode(),
                                errorCode.getDefaultMessage(),
                                request.getRequestURI(),
                                violations));
    }
}
