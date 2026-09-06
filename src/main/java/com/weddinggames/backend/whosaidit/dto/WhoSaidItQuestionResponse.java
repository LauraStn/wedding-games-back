package com.weddinggames.backend.whosaidit.dto;

import com.weddinggames.backend.whosaidit.WhoSaidItQuestion;
import com.weddinggames.backend.whosaidit.WhoSaidItQuestionStatus;
import java.time.Instant;
import java.util.UUID;

public record WhoSaidItQuestionResponse(
        UUID id,
        UUID eventId,
        UUID authorId,
        String authorDisplayName,
        String content,
        WhoSaidItQuestionStatus status,
        boolean revealAuthorConsent,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * {@code authorDisplayName} is populated only when the author consented to being revealed -
     * except for staff, who can already see {@code authorId} unconditionally (for moderation
     * accountability) and so gain nothing from the name being hidden too.
     */
    public static WhoSaidItQuestionResponse from(WhoSaidItQuestion question) {
        return from(question, false);
    }

    public static WhoSaidItQuestionResponse forStaff(WhoSaidItQuestion question) {
        return from(question, true);
    }

    private static WhoSaidItQuestionResponse from(WhoSaidItQuestion question, boolean revealAuthorAlways) {
        boolean revealName = revealAuthorAlways || question.isRevealAuthorConsent();
        return new WhoSaidItQuestionResponse(
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
