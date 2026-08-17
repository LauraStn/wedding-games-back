package com.weddinggames.backend.invitation.dto;

import com.weddinggames.backend.invitation.Invitation;
import com.weddinggames.backend.invitation.InvitationStatus;
import java.time.Instant;
import java.util.UUID;

public record InvitationStatusResponse(UUID invitationId, InvitationStatus status, Instant createdAt) {

    public static InvitationStatusResponse from(Invitation invitation) {
        return new InvitationStatusResponse(invitation.getId(), invitation.getStatus(), invitation.getCreatedAt());
    }
}
