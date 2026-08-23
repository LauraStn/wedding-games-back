package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An append-only ledger entry awarding (or removing) points from a team, e.g. after a round or the final podium. */
@Entity
@Table(name = "score")
public class Score extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private int points;

    @Column(length = 200)
    private String reason;

    protected Score() {}

    public Score(WeddingEvent event, Game game, Team team, int points, String reason) {
        this.event = event;
        this.game = game;
        this.team = team;
        this.points = points;
        this.reason = reason;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public Game getGame() {
        return game;
    }

    public Team getTeam() {
        return team;
    }

    public int getPoints() {
        return points;
    }

    public String getReason() {
        return reason;
    }
}
