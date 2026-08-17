package com.weddinggames.backend.lobby.dto;

import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyStatus;
import java.time.Instant;
import java.util.UUID;

public record LobbyResponse(UUID id, UUID eventId, LobbyStatus status, Instant openedAt, Instant closedAt) {

    public static LobbyResponse from(Lobby lobby) {
        return new LobbyResponse(
                lobby.getId(), lobby.getEvent().getId(), lobby.getStatus(), lobby.getOpenedAt(), lobby.getClosedAt());
    }
}
