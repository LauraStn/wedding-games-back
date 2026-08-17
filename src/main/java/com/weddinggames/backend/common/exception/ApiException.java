package com.weddinggames.backend.common.exception;

/**
 * Base class for business exceptions that carry a stable technical code
 * used by the frontend to react programmatically to specific error cases.
 */
public abstract class ApiException extends RuntimeException {

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
