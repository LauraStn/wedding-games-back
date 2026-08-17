package com.weddinggames.backend.participant.dto;

import com.weddinggames.backend.participant.ParticipantStatus;
import com.weddinggames.backend.participant.ParticipantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParticipantUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 150) String displayName,
        @Size(max = 50) String tableLabel,
        @NotNull ParticipantType participantType,
        @NotNull ParticipantStatus status) {}
