package com.weddinggames.backend.vote.dto;

import com.weddinggames.backend.vote.FinalistService;
import java.util.UUID;

public record FinalistResponse(UUID answerId, String content, Long voteCount) {

    public static FinalistResponse from(FinalistService.Finalist finalist, boolean revealVoteCount) {
        return new FinalistResponse(
                finalist.answer().getId(),
                finalist.answer().getContent(),
                revealVoteCount ? finalist.voteCount() : null);
    }
}
