package com.weddinggames.backend.lobby;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.event.WeddingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lobby")
public class Lobby extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LobbyStatus status = LobbyStatus.CLOSED;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Lobby() {}

    public Lobby(WeddingEvent event) {
        this.event = event;
    }

    public void open(Instant now) {
        this.status = LobbyStatus.OPEN;
        this.openedAt = now;
        this.closedAt = null;
    }

    public void close(Instant now) {
        this.status = LobbyStatus.CLOSED;
        this.closedAt = now;
    }

    public void lock() {
        this.status = LobbyStatus.LOCKED;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public LobbyStatus getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
