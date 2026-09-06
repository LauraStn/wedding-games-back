package com.weddinggames.backend.blindtest.dto;

import jakarta.validation.constraints.Min;

public record BlindTestFormatRequest(
        @Min(1) int roundDurationSeconds, @Min(0) int pointsPerCorrectGuess) {}
