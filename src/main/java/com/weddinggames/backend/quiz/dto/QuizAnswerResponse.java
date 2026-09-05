package com.weddinggames.backend.quiz.dto;

import com.weddinggames.backend.game.Answer;
import java.time.Instant;
import java.util.UUID;

public record QuizAnswerResponse(
        UUID questionId,
        UUID teamId,
        String content,
        UUID controllingParticipantId,
        String controllingParticipantName,
        Instant lastEditedAt) {

    public static QuizAnswerResponse from(Answer answer) {
        var controlling = answer.getControllingParticipant();
        return new QuizAnswerResponse(
                answer.getQuestion().getId(),
                answer.getTeam().getId(),
                answer.getContent(),
                controlling != null ? controlling.getId() : null,
                controlling != null ? controlling.getDisplayName() : null,
                answer.getSubmittedAt());
    }
}
