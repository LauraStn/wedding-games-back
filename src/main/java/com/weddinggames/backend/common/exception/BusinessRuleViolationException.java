package com.weddinggames.backend.common.exception;

/**
 * Raised when an operation is refused not because of authorization,
 * but because it would violate an absolute business invariant
 * (e.g. deleting a HARD pairing exclusion).
 */
public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
