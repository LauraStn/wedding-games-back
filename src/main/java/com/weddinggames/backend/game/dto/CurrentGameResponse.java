package com.weddinggames.backend.game.dto;

import com.weddinggames.backend.game.CurrentGameService.CurrentGameState;

/**
 * What the room is playing right now, for a guest's screen. Both fields are {@code null} when no
 * game is live; {@code question} alone is {@code null} while a live game is still in preparation.
 */
public record CurrentGameResponse(GameResponse game, CurrentQuestionResponse question) {

    public static CurrentGameResponse from(CurrentGameState state) {
        return new CurrentGameResponse(
                state.game() == null ? null : GameResponse.from(state.game()),
                state.question() == null ? null : CurrentQuestionResponse.from(state.question()));
    }
}
