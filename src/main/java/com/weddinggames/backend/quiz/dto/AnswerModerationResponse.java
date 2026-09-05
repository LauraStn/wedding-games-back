package com.weddinggames.backend.quiz.dto;

import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record AnswerModerationResponse(
        UUID id,
        UUID questionId,
        UUID teamId,
        String teamLabel,
        String content,
        AnswerModerationStatus moderationStatus,
        UUID controllingParticipantId,
        Instant lastEditedAt) {

    public static AnswerModerationResponse from(Answer answer) {
        var controlling = answer.getControllingParticipant();
        return new AnswerModerationResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getTeam().getId(),
                answer.getTeam().getLabel(),
                answer.getContent(),
                answer.getModerationStatus(),
                controlling != null ? controlling.getId() : null,
                answer.getSubmittedAt());
    }
}
