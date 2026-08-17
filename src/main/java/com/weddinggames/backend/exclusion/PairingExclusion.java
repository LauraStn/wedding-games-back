package com.weddinggames.backend.exclusion;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a matchmaking exclusion between exactly two participants of the same event.
 * Participants are stored in a normalized order (participantA.id &lt; participantB.id, as
 * UUID string comparison) so the same pair can never be recorded twice regardless of the
 * order it was submitted in. This is the sole mechanism used to express "these two people
 * must never be paired" - never a name comparison in code.
 */
@Entity
@Table(name = "pairing_exclusion")
public class PairingExclusion extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_a_id", nullable = false)
    private Participant participantA;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_b_id", nullable = false)
    private Participant participantB;

    @Column(length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_type", nullable = false, length = 20)
    private ExclusionType exclusionType;

    @Column(nullable = false)
    private boolean locked;

    protected PairingExclusion() {}

    public PairingExclusion(
            WeddingEvent event,
            Participant participantA,
            Participant participantB,
            String reason,
            ExclusionType exclusionType) {
        this.event = event;
        this.participantA = participantA;
        this.participantB = participantB;
        this.reason = reason;
        this.exclusionType = exclusionType;
        this.locked = exclusionType == ExclusionType.HARD;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public Participant getParticipantA() {
        return participantA;
    }

    public Participant getParticipantB() {
        return participantB;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ExclusionType getExclusionType() {
        return exclusionType;
    }

    public boolean isLocked() {
        return locked;
    }
}
