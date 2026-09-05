package com.weddinggames.backend.matchmaking.dto;

import com.weddinggames.backend.team.TeamMember;
import java.util.UUID;

public record TeamMemberResponse(UUID participantId, String displayName, UUID characterId, String characterName) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getParticipant().getId(),
                member.getParticipant().getDisplayName(),
                member.getCharacter() != null ? member.getCharacter().getId() : null,
                member.getCharacter() != null ? member.getCharacter().getName() : null);
    }
}
