package com.weddinggames.backend.game.dto;

import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionStatus;
import java.util.UUID;

/**
 * The current question as a guest needs to see it: its id (to address {@code /quiz/...} or
 * {@code /vote/...}), where it sits in the game, its status ({@code ACTIVE} = answer, {@code CLOSED}
 * = vote) and its prompt. Deliberately omits the author/source of the question.
 */
public record CurrentQuestionResponse(UUID id, int sequence, QuestionStatus status, String prompt) {

    public static CurrentQuestionResponse from(Question question) {
        return new CurrentQuestionResponse(
                question.getId(), question.getSequence(), question.getStatus(), question.getPrompt());
    }
}
