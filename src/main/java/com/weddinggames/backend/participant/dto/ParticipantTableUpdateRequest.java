package com.weddinggames.backend.participant.dto;

import jakarta.validation.constraints.Size;

public record ParticipantTableUpdateRequest(@Size(max = 50) String tableLabel) {}
