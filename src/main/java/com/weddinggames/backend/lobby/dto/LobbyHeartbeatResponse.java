package com.weddinggames.backend.lobby.dto;

import com.weddinggames.backend.lobby.LobbyConnectionStatus;
import com.weddinggames.backend.lobby.LobbyParticipant;
import java.time.Instant;

/**
 * Self-facing acknowledgement of a heartbeat/ready call. Deliberately narrower than
 * {@link LobbyParticipantResponse}: the duplicate/QR-reuse signals it carries are for staff
 * only and must never be echoed back to the participant they're about.
 */
public record LobbyHeartbeatResponse(LobbyConnectionStatus connectionStatus, Instant lastActivityAt) {

    public static LobbyHeartbeatResponse from(LobbyParticipant lobbyParticipant) {
        return new LobbyHeartbeatResponse(
                lobbyParticipant.getConnectionStatus(), lobbyParticipant.getLastActivityAt());
    }
}
