package com.weddinggames.backend.participant.dto;

import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantStatus;
import com.weddinggames.backend.participant.ParticipantType;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
        UUID id,
        UUID eventId,
        String firstName,
        String lastName,
        String displayName,
        String tableLabel,
        ParticipantType participantType,
        ParticipantStatus status,
        int totalPoints,
        int totalWins,
        Instant createdAt,
        Instant updatedAt) {

    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getEvent().getId(),
                participant.getFirstName(),
                participant.getLastName(),
                participant.getDisplayName(),
                participant.getTableLabel(),
                participant.getParticipantType(),
                participant.getStatus(),
                participant.getTotalPoints(),
                participant.getTotalWins(),
                participant.getCreatedAt(),
                participant.getUpdatedAt());
    }
}
