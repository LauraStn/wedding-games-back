package com.weddinggames.backend.team.dto;

import com.weddinggames.backend.team.TeamMember;
import java.util.UUID;

public record TeammateResponse(
        UUID participantId, String displayName, UUID characterId, String characterName, String characterAvatarUrl) {

    public static TeammateResponse from(TeamMember member) {
        return new TeammateResponse(
                member.getParticipant().getId(),
                member.getParticipant().getDisplayName(),
                member.getCharacter() != null ? member.getCharacter().getId() : null,
                member.getCharacter() != null ? member.getCharacter().getName() : null,
                member.getCharacter() != null ? member.getCharacter().getAvatarUrl() : null);
    }
}
