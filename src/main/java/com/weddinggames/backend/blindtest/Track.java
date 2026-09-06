package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.game.Game;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A song configured for a "blind test" game, played in one of a few guessing variants. */
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
}
