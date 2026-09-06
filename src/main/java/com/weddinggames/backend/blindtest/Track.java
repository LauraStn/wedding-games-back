package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Game;
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
 * A song configured for a "blind test" game, played in one of a few guessing variants.
 *
 * <p>Round lifecycle: PENDING -&gt; ACTIVE -&gt; CLOSED, same shape as {@code Question}. The
 * countdown timer is a separate action from activating the round: activating just makes the
 * track the "current" one so staff/projection can show its title/variant, while starting the
 * timer is the intervenant's explicit cue that guessing has begun.
 */
@Entity
@Table(name = "track")
public class Track extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlindTestVariant variant;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackStatus status = TrackStatus.PENDING;

    @Column(name = "timer_started_at")
    private Instant timerStartedAt;

    protected Track() {}

    public Track(Game game, String title, String artist, BlindTestVariant variant, int sequence) {
        this.game = game;
        this.title = title;
        this.artist = artist;
        this.variant = variant;
        this.sequence = sequence;
    }

    public Game getGame() {
        return game;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public BlindTestVariant getVariant() {
        return variant;
    }

    public void setVariant(BlindTestVariant variant) {
        this.variant = variant;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public Instant getTimerStartedAt() {
        return timerStartedAt;
    }

    public void activate() {
        if (status != TrackStatus.PENDING) {
            throw new BusinessRuleViolationException(
                    "INVALID_TRACK_STATUS_TRANSITION",
                    "Seul un morceau en attente (PENDING) peut etre active, statut actuel: " + status + ".");
        }
        status = TrackStatus.ACTIVE;
    }

    public void close() {
        if (status != TrackStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "INVALID_TRACK_STATUS_TRANSITION",
                    "Seul un morceau actif peut etre ferme, statut actuel: " + status + ".");
        }
        status = TrackStatus.CLOSED;
    }

    public void startTimer(Instant now) {
        if (status != TrackStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "TRACK_NOT_ACTIVE", "Le chronometre ne peut etre lance que sur un morceau actif.");
        }
        timerStartedAt = now;
    }
}
