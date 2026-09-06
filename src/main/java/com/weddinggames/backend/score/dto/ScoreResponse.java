package com.weddinggames.backend.score.dto;

import com.weddinggames.backend.game.Score;
import java.time.Instant;
import java.util.UUID;

public record ScoreResponse(
        UUID id, UUID eventId, UUID gameId, UUID teamId, int points, String reason, Instant createdAt) {

    public static ScoreResponse from(Score score) {
        return new ScoreResponse(
                score.getId(),
                score.getEvent().getId(),
                score.getGame() == null ? null : score.getGame().getId(),
                score.getTeam().getId(),
                score.getPoints(),
                score.getReason(),
                score.getCreatedAt());
    }
}
