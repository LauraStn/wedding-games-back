package com.weddinggames.backend.participant.dto;

import com.weddinggames.backend.participant.ParticipantType;

public record ParticipantImportRow(
        int rowNumber,
        String firstName,
        String lastName,
        String displayName,
        String tableLabel,
        ParticipantType participantType,
        ParticipantImportRowStatus status,
        String rejectionReason) {}
