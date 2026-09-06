package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.game.Game;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Round format configuration for a "blind test" game: one per game, created on first access. */
@Entity
@Table(name = "blind_test_format")
public class BlindTestFormat extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Column(name = "round_duration_seconds", nullable = false)
    private int roundDurationSeconds = 30;

    @Column(name = "points_per_correct_guess", nullable = false)
    private int pointsPerCorrectGuess = 10;

    protected BlindTestFormat() {}

    public BlindTestFormat(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }

    public void setRoundDurationSeconds(int roundDurationSeconds) {
        this.roundDurationSeconds = roundDurationSeconds;
    }

    public int getPointsPerCorrectGuess() {
        return pointsPerCorrectGuess;
    }

    public void setPointsPerCorrectGuess(int pointsPerCorrectGuess) {
        this.pointsPerCorrectGuess = pointsPerCorrectGuess;
    }
}
