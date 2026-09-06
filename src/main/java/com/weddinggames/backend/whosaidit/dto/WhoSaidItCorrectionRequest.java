package com.weddinggames.backend.whosaidit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Staff fixing a typo or garbled text, without changing the meaning or the author's consent. */
public record WhoSaidItCorrectionRequest(@NotBlank @Size(max = 500) String content) {}
