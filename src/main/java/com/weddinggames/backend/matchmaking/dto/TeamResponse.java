package com.weddinggames.backend.matchmaking.dto;

import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import java.util.List;
import java.util.UUID;

public record TeamResponse(UUID id, String label, List<TeamMemberResponse> members) {

    public static TeamResponse from(Team team, List<TeamMember> members) {
        return new TeamResponse(
                team.getId(), team.getLabel(), members.stream().map(TeamMemberResponse::from).toList());
    }
}
