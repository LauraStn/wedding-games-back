package com.weddinggames.backend.blindtest.dto;

import com.weddinggames.backend.blindtest.BlindTestVariant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrackCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String artist,
        @NotNull BlindTestVariant variant,
        int sequence) {}
