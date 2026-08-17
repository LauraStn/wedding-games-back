package com.weddinggames.backend.exclusion.dto;

import com.weddinggames.backend.exclusion.ExclusionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PairingExclusionCreateRequest(
        @NotNull UUID participantAId,
        @NotNull UUID participantBId,
        @Size(max = 300) String reason,
        @NotNull ExclusionType exclusionType) {}
