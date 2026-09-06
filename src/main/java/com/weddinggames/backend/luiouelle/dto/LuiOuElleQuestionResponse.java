package com.weddinggames.backend.luiouelle.dto;

import com.weddinggames.backend.luiouelle.LuiOuElleQuestion;
import java.time.Instant;
import java.util.UUID;

public record LuiOuElleQuestionResponse(
        UUID id, UUID eventId, UUID authorId, String content, Instant createdAt, Instant updatedAt) {

    public static LuiOuElleQuestionResponse from(LuiOuElleQuestion question) {
        return new LuiOuElleQuestionResponse(
                question.getId(),
                question.getEvent().getId(),
                question.getAuthor().getId(),
                question.getContent(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
