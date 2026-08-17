package com.weddinggames.backend.exclusion.dto;

import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingExclusion;
import java.time.Instant;
import java.util.UUID;

public record PairingExclusionResponse(
        UUID id,
        UUID eventId,
        UUID participantAId,
        UUID participantBId,
        String reason,
        ExclusionType exclusionType,
        boolean locked,
        Instant createdAt) {

    public static PairingExclusionResponse from(PairingExclusion exclusion) {
        return new PairingExclusionResponse(
                exclusion.getId(),
                exclusion.getEvent().getId(),
                exclusion.getParticipantA().getId(),
                exclusion.getParticipantB().getId(),
                exclusion.getReason(),
                exclusion.getExclusionType(),
                exclusion.isLocked(),
                exclusion.getCreatedAt());
    }
}
