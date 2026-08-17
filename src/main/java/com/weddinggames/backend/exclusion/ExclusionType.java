package com.weddinggames.backend.exclusion;

public enum ExclusionType {
    /** Absolute: can never be bypassed, by anyone, through the API. */
    HARD,
    /** Advisory: the future matchmaking algorithm should try to respect it, but may override it. */
    PREFERENCE
}
