package com.weddinggames.backend.security;

import com.weddinggames.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Opaque server-side session shared by participants and staff members.
 * Only the SHA-256 hash of the raw token is ever persisted; the raw value
 * lives solely in the HttpOnly cookie sent to the client.
 */
@Entity
@Table(name = "app_session")
public class AppSession extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "staff_account_id")
    private UUID staffAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "session_token_hash", nullable = false, unique = true, length = 128)
    private String sessionTokenHash;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AppSession() {}

    public AppSession(
            ActorType actorType,
            UUID participantId,
            UUID staffAccountId,
            Role role,
            String sessionTokenHash,
            Instant expiresAt) {
        this.actorType = actorType;
        this.participantId = participantId;
        this.staffAccountId = staffAccountId;
        this.role = role;
        this.sessionTokenHash = sessionTokenHash;
        this.lastSeenAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isValid(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void touch(Instant now) {
        this.lastSeenAt = now;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public UUID getStaffAccountId() {
        return staffAccountId;
    }

    public Role getRole() {
        return role;
    }

    public String getSessionTokenHash() {
        return sessionTokenHash;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
