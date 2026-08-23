package com.weddinggames.backend.game;

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

/** A configured "jeu" (quiz, lui-ou-elle, blind test...) attached to an event. */
@Entity
@Table(name = "game")
public class Game extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int sequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.DRAFT;

    protected Game() {}

    public Game(WeddingEvent event, GameType type, String title, int sequence) {
        this.event = event;
        this.type = type;
        this.title = title;
        this.sequence = sequence;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public GameType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}
