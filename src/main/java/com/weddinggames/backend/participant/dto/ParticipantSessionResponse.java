package com.weddinggames.backend.participant.dto;

import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantStatus;
import java.util.UUID;

/** What a participant sees about their own session: identity, presence, and score to date. */
public record ParticipantSessionResponse(
        UUID participantId,
        UUID eventId,
        String eventSlug,
        String firstName,
        String displayName,
        ParticipantStatus status,
        int totalPoints,
        int totalWins) {

    public static ParticipantSessionResponse from(Participant participant) {
        return new ParticipantSessionResponse(
                participant.getId(),
                participant.getEvent().getId(),
                participant.getEvent().getSlug(),
                participant.getFirstName(),
                participant.getDisplayName(),
                participant.getStatus(),
                participant.getTotalPoints(),
                participant.getTotalWins());
    }
}
