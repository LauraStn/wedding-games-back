package com.weddinggames.backend.game.dto;

import com.weddinggames.backend.game.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GameCreateRequest(
        @NotNull GameType type, @NotBlank @Size(max = 200) String title, int sequence) {}
