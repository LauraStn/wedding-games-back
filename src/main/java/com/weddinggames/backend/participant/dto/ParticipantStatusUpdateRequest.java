package com.weddinggames.backend.participant.dto;

import com.weddinggames.backend.participant.ParticipantStatus;
import jakarta.validation.constraints.NotNull;

public record ParticipantStatusUpdateRequest(@NotNull ParticipantStatus status) {}
