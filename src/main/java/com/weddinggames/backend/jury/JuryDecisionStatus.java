package com.weddinggames.backend.jury;

public enum JuryDecisionStatus {
    /** No winning answer picked yet. */
    PENDING,
    /** An answer is picked but not yet confirmed - can still be changed. */
    CHOSEN,
    /** Confirmed: the round's points have been awarded, the choice is final. */
    CONFIRMED
}
