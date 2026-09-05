package com.weddinggames.backend.vote.dto;

import com.weddinggames.backend.game.Answer;
import java.util.UUID;

/** Anonymized on purpose: no team name, no character, no participant identity - just the text. */
public record VotingOptionResponse(UUID answerId, String content) {

    public static VotingOptionResponse from(Answer answer) {
        return new VotingOptionResponse(answer.getId(), answer.getContent());
    }
}
