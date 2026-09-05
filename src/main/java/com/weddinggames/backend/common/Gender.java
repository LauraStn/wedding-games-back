package com.weddinggames.backend.common;

/**
 * Optional tag on a participant or a character, used only to bias the matchmaking's character
 * assignment towards a same-gender match when both sides are tagged. Absent (null) on either
 * side means "no preference" - matching never blocks the game, it only prefers a match.
 */
public enum Gender {
    MALE,
    FEMALE
}
