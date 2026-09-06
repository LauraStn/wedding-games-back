package com.weddinggames.backend.whosaidit.dto;

import jakarta.validation.constraints.NotBlank;

/** A guest proposing or editing their own question, including their author-reveal consent. */
public record WhoSaidItQuestionRequest(@NotBlank String content, boolean revealAuthorConsent) {}
