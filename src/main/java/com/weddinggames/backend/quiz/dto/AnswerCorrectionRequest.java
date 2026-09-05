package com.weddinggames.backend.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Staff fixing a typo or garbled text, without changing the meaning of the answer. */
public record AnswerCorrectionRequest(@NotBlank @Size(max = 1000) String content) {}
