package com.weddinggames.backend.vote.dto;

import com.weddinggames.backend.game.Vote;
import java.util.UUID;

public record VoteResponse(UUID id, UUID questionId, UUID answerId) {

    public static VoteResponse from(Vote vote) {
        return new VoteResponse(vote.getId(), vote.getQuestion().getId(), vote.getAnswer().getId());
    }
}
