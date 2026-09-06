package com.weddinggames.backend.luiouelle.dto;

import jakarta.validation.constraints.NotBlank;

/** A guest proposing or editing their own question, including their author-reveal consent. */
public record LuiOuElleQuestionRequest(@NotBlank String content, boolean revealAuthorConsent) {}
