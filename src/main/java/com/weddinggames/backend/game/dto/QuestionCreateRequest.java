package com.weddinggames.backend.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(@NotBlank @Size(max = 1000) String prompt, int sequence) {}
