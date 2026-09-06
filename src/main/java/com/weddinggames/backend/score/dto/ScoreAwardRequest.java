package com.weddinggames.backend.score.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ScoreAwardRequest(
        UUID gameId, @NotNull UUID teamId, int points, @Size(max = 200) String reason) {}
