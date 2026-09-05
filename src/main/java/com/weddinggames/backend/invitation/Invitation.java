package com.weddinggames.backend.invitation;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Only the hash of the invitation token is ever persisted. The raw token is
 * returned exactly once, in the admin generate/regenerate response, and is
 * embedded (never the participant's name) in the QR code the frontend renders.
 */
@Entity
@Table(name = "invitation")
public class Invitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    /** Plain text on purpose: unlike the token, an administrator must be able to read it back to a guest. */
    @Column(name = "fallback_code", unique = true, length = 10)
    private String fallbackCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.ACTIVE;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Invitation() {}

    public Invitation(Participant participant, String tokenHash) {
        this.participant = participant;
        this.tokenHash = tokenHash;
    }

    public void revoke(Instant now) {
        this.status = InvitationStatus.REVOKED;
        this.revokedAt = now;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getFallbackCode() {
        return fallbackCode;
    }

    public void setFallbackCode(String fallbackCode) {
        this.fallbackCode = fallbackCode;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
