package com.weddinggames.backend.luiouelle.dto;

import jakarta.validation.constraints.NotBlank;

public record LuiOuElleQuestionRequest(@NotBlank String content) {}
