package com.weddinggames.backend.luiouelle.dto;

import com.weddinggames.backend.luiouelle.LuiOuElleQuestion;
import com.weddinggames.backend.luiouelle.LuiOuElleQuestionStatus;
import java.time.Instant;
import java.util.UUID;

public record LuiOuElleQuestionResponse(
        UUID id,
        UUID eventId,
        UUID authorId,
        String authorDisplayName,
        String content,
        LuiOuElleQuestionStatus status,
        boolean revealAuthorConsent,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * {@code authorDisplayName} is populated only when the author consented to being revealed -
     * except for staff, who can already see {@code authorId} unconditionally (for moderation
     * accountability) and so gain nothing from the name being hidden too.
     */
    public static LuiOuElleQuestionResponse from(LuiOuElleQuestion question) {
        return from(question, false);
    }

    public static LuiOuElleQuestionResponse forStaff(LuiOuElleQuestion question) {
        return from(question, true);
    }

    private static LuiOuElleQuestionResponse from(LuiOuElleQuestion question, boolean revealAuthorAlways) {
        boolean revealName = revealAuthorAlways || question.isRevealAuthorConsent();
        return new LuiOuElleQuestionResponse(
                question.getId(),
                question.getEvent().getId(),
                question.getAuthor().getId(),
                revealName ? question.getAuthor().getDisplayName() : null,
                question.getContent(),
                question.getStatus(),
                question.isRevealAuthorConsent(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
