package com.weddinggames.backend.game;

public enum AnswerModerationStatus {
    /** Not yet reviewed by staff. */
    PENDING,
    /** Approved: eligible for projection, vote and jury. */
    ACCEPTED,
    /**
     * Masked ("masquer") or refused for inappropriate content ("refuser un contenu
     * humiliant/intime") - either way, the outcome the acceptance criteria cares about is the
     * same: never shown to projection, vote or jury. Any future endpoint that surfaces answers
     * to guests must filter on {@code moderationStatus == ACCEPTED}, never on the absence of
     * HIDDEN alone (PENDING must not leak either).
     */
    HIDDEN
}
