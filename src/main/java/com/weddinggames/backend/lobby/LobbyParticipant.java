package com.weddinggames.backend.lobby;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
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
 * Presence record of a participant inside a lobby. Phase 1 uses simple REST polling and a
 * heartbeat endpoint; lastActivityAt is the field a future real-time layer (WebSocket/SSE)
 * would keep fresh instead.
 */
@Entity
@Table(name = "lobby_participant")
public class LobbyParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "arrived_at", nullable = false)
    private Instant arrivedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 20)
    private LobbyConnectionStatus connectionStatus;

    protected LobbyParticipant() {}

    public LobbyParticipant(Lobby lobby, Participant participant, Instant now) {
        this.lobby = lobby;
        this.participant = participant;
        this.arrivedAt = now;
        this.lastActivityAt = now;
        this.connectionStatus = LobbyConnectionStatus.CONNECTED;
    }

    public void heartbeat(Instant now) {
        this.lastActivityAt = now;
        // A routine presence ping must not silently downgrade an explicit "I'm ready" declaration.
        if (this.connectionStatus != LobbyConnectionStatus.READY) {
            this.connectionStatus = LobbyConnectionStatus.CONNECTED;
        }
    }

    public void markReady(Instant now) {
        this.lastActivityAt = now;
        this.connectionStatus = LobbyConnectionStatus.READY;
    }

    public void markLate() {
        this.connectionStatus = LobbyConnectionStatus.LATE;
    }

    public void admit(Instant now) {
        this.lastActivityAt = now;
        this.connectionStatus = LobbyConnectionStatus.CONNECTED;
    }

    public void markDisconnected() {
        this.connectionStatus = LobbyConnectionStatus.DISCONNECTED;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public Participant getParticipant() {
        return participant;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public LobbyConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
}
