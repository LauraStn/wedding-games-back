package com.weddinggames.backend.security;

import java.util.UUID;

/**
 * Authentication principal resolved from an opaque session cookie.
 * Carries just enough identity for controllers/services to act on behalf
 * of the caller without depending on the security package's entities.
 */
public record AuthenticatedActor(
        UUID sessionId, ActorType actorType, UUID participantId, UUID staffAccountId, Role role) {

    public boolean isParticipant() {
        return actorType == ActorType.PARTICIPANT;
    }

    public boolean isStaff() {
        return actorType == ActorType.STAFF;
    }
}
