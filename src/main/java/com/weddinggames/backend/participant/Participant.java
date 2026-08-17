package com.weddinggames.backend.participant;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "participant")
public class Participant extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "table_label", length = 50)
    private String tableLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status = ParticipantStatus.INVITED;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "total_wins", nullable = false)
    private int totalWins = 0;

    protected Participant() {}

    public Participant(
            WeddingEvent event,
            String firstName,
            String lastName,
            String displayName,
            String tableLabel,
            ParticipantType participantType) {
        this.event = event;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.tableLabel = tableLabel;
        this.participantType = participantType;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTableLabel() {
        return tableLabel;
    }

    public void setTableLabel(String tableLabel) {
        this.tableLabel = tableLabel;
    }

    public ParticipantType getParticipantType() {
        return participantType;
    }

    public void setParticipantType(ParticipantType participantType) {
        this.participantType = participantType;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }
}
