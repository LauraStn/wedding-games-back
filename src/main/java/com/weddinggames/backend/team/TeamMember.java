package com.weddinggames.backend.team;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Membership of a participant in a team. A participant belongs to at most one team at a time.
 * Carries its own individual character assignment: two members of the same binôme/trio always
 * have two different characters.
 */
@Entity
@Table(name = "team_member")
public class TeamMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    protected TeamMember() {}

    public TeamMember(Team team, Participant participant) {
        this.team = team;
        this.participant = participant;
    }

    public Team getTeam() {
        return team;
    }

    public Participant getParticipant() {
        return participant;
    }

    public GameCharacter getCharacter() {
        return character;
    }

    public void setCharacter(GameCharacter character) {
        this.character = character;
    }
}
