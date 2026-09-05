package com.weddinggames.backend.game.dto;

import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionSource;
import com.weddinggames.backend.game.QuestionStatus;
import java.util.UUID;

public record QuestionResponse(
        UUID id, UUID gameId, String prompt, int sequence, QuestionStatus status, QuestionSource source) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getGame().getId(),
                question.getPrompt(),
                question.getSequence(),
                question.getStatus(),
                question.getSource());
    }
}
