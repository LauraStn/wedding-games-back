package com.weddinggames.backend.matchmaking.dto;

import com.weddinggames.backend.participant.Participant;
import java.util.UUID;

/** Another latecomer, not yet on any team, who could be paired with this one into a new binôme. */
public record LatecomerCandidateResponse(UUID participantId, String displayName) {

    public static LatecomerCandidateResponse from(Participant participant) {
        return new LatecomerCandidateResponse(participant.getId(), participant.getDisplayName());
    }
}
