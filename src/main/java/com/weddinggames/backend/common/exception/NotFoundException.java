package com.weddinggames.backend.common.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
