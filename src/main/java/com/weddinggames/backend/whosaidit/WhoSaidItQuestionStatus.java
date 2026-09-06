package com.weddinggames.backend.whosaidit;

public enum WhoSaidItQuestionStatus {
    /** Just proposed by a guest, not yet reviewed by staff. */
    PENDING,
    /** Approved by staff: eligible for random selection during play (see the game-side ticket). */
    ACCEPTED,
    /** Refused by staff: never selected for play. */
    REJECTED,
    /** Selected and used during play. Terminal: a played question is never un-played. */
    PLAYED
}
