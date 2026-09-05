package com.weddinggames.backend.lobby;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
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
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "lobby")
public class Lobby extends BaseEntity {

    /**
     * Lifecycle: CLOSED <-> OPEN <-> LOCKED -> ACTIVE <-> PAUSED -> FINISHED (terminal).
     * FINISHED has no way out on purpose: once a lobby's session is over, it stays over.
     */
    private static final Map<LobbyStatus, Set<LobbyStatus>> ALLOWED_PREDECESSORS = Map.of(
            // Self-loops on the three pre-session states so an accidental double-click (open/lock/close
            // again) is a harmless no-op rather than a hard error for the intervenant.
            LobbyStatus.OPEN, Set.of(LobbyStatus.CLOSED, LobbyStatus.LOCKED, LobbyStatus.OPEN),
            LobbyStatus.LOCKED, Set.of(LobbyStatus.OPEN, LobbyStatus.LOCKED),
            LobbyStatus.CLOSED, Set.of(LobbyStatus.OPEN, LobbyStatus.LOCKED, LobbyStatus.CLOSED),
            LobbyStatus.ACTIVE, Set.of(LobbyStatus.LOCKED, LobbyStatus.PAUSED),
            LobbyStatus.PAUSED, Set.of(LobbyStatus.ACTIVE),
            LobbyStatus.FINISHED, Set.of(LobbyStatus.ACTIVE, LobbyStatus.PAUSED));

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
        transitionTo(LobbyStatus.OPEN);
        this.openedAt = now;
        this.closedAt = null;
    }

    public void close(Instant now) {
        transitionTo(LobbyStatus.CLOSED);
        this.closedAt = now;
    }

    public void lock() {
        transitionTo(LobbyStatus.LOCKED);
    }

    public void start() {
        transitionTo(LobbyStatus.ACTIVE);
    }

    public void pause() {
        transitionTo(LobbyStatus.PAUSED);
    }

    public void resume() {
        transitionTo(LobbyStatus.ACTIVE);
    }

    public void finish() {
        transitionTo(LobbyStatus.FINISHED);
    }

    private void transitionTo(LobbyStatus target) {
        if (!ALLOWED_PREDECESSORS.get(target).contains(this.status)) {
            throw new BusinessRuleViolationException(
                    "INVALID_LOBBY_TRANSITION",
                    "Transition de salon invalide: " + this.status + " -> " + target + ".");
        }
        this.status = target;
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
