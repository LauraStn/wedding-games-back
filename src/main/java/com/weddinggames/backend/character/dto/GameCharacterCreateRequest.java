package com.weddinggames.backend.character.dto;

import com.weddinggames.backend.common.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameCharacterCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String avatarUrl,
        Gender gender) {}
