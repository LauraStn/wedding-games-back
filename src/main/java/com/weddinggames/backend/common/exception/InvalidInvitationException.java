package com.weddinggames.backend.common.exception;

public class InvalidInvitationException extends ApiException {

    public InvalidInvitationException(String message) {
        super("INVALID_INVITATION", message);
    }
}
