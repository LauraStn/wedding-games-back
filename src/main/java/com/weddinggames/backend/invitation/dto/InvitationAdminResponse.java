package com.weddinggames.backend.invitation.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned exactly once, right after generation/regeneration: the only moment the
 * raw token is ever visible. It is never stored and cannot be retrieved again.
 */
public record InvitationAdminResponse(
        UUID invitationId,
        UUID participantId,
        String rawToken,
        String invitationUrl,
        String fallbackCode,
        Instant createdAt) {}
