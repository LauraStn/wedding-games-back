package com.weddinggames.backend.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizAnswerUpdateRequest(@NotBlank @Size(max = 1000) String content) {}
