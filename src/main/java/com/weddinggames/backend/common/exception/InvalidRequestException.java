package com.weddinggames.backend.common.exception;

public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String code, String message) {
        super(code, message);
    }
}
