package com.weddinggames.backend.game.dto;

import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GamePhase;
import com.weddinggames.backend.game.GameStatus;
import com.weddinggames.backend.game.GameType;
import java.util.UUID;

public record GameResponse(
        UUID id, UUID eventId, GameType type, String title, int sequence, GameStatus status, GamePhase phase) {

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getEvent().getId(),
                game.getType(),
                game.getTitle(),
                game.getSequence(),
                game.getStatus(),
                game.getPhase());
    }
}
