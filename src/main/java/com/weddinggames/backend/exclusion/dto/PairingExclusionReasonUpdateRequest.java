package com.weddinggames.backend.exclusion.dto;

import jakarta.validation.constraints.Size;

public record PairingExclusionReasonUpdateRequest(@Size(max = 300) String reason) {}
