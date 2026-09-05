package com.weddinggames.backend.team;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A matchmaking pairing (binôme or trio) of participants for an event. Each member gets their
 * own individual character (see {@link TeamMember#getCharacter()}) - the team itself carries no
 * character, since a binôme is made of two people with two different, complementary characters.
 */
@Entity
@Table(name = "team")
public class Team extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Column(length = 100)
    private String label;

    protected Team() {}

    public Team(WeddingEvent event) {
        this.event = event;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
