package edu.hcmute.careersim.common.exception;

import java.util.Map;

public final class DomainValidationException extends ApiException {
    private final Map<String, String> fields;

    public DomainValidationException(String message, Map<String, String> fields) {
        super(ErrorCode.DOMAIN_VALIDATION, message);
        this.fields = Map.copyOf(fields);
    }

    public Map<String, String> fields() {
        return fields;
    }
}
