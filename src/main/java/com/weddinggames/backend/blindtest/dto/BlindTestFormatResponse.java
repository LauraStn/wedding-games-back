package com.weddinggames.backend.blindtest.dto;

import com.weddinggames.backend.blindtest.BlindTestFormat;
import java.util.UUID;

public record BlindTestFormatResponse(
        UUID gameId, int roundDurationSeconds, int pointsPerCorrectGuess) {

    public static BlindTestFormatResponse from(BlindTestFormat format) {
        return new BlindTestFormatResponse(
                format.getGame().getId(), format.getRoundDurationSeconds(), format.getPointsPerCorrectGuess());
    }
}
