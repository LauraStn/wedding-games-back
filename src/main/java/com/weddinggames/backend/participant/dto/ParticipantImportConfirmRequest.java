package com.weddinggames.backend.participant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ParticipantImportConfirmRequest(@NotEmpty @Valid List<ParticipantImportRowInput> rows) {}
