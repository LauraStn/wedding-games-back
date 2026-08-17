package com.weddinggames.backend.lobby.dto;

import com.weddinggames.backend.lobby.LobbyConnectionStatus;
import com.weddinggames.backend.lobby.LobbyParticipant;
import java.time.Instant;
import java.util.UUID;

public record LobbyParticipantResponse(
        UUID participantId,
        String displayName,
        LobbyConnectionStatus connectionStatus,
        Instant arrivedAt,
        Instant lastActivityAt) {

    public static LobbyParticipantResponse from(LobbyParticipant lobbyParticipant) {
        return new LobbyParticipantResponse(
                lobbyParticipant.getParticipant().getId(),
                lobbyParticipant.getParticipant().getDisplayName(),
                lobbyParticipant.getConnectionStatus(),
                lobbyParticipant.getArrivedAt(),
                lobbyParticipant.getLastActivityAt());
    }
}
