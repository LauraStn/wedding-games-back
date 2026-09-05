package com.weddinggames.backend.team;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.team.dto.MyTeamResponse;
import com.weddinggames.backend.team.dto.TeammateResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamParticipantService {

    private final TeamMemberRepository teamMemberRepository;

    public TeamParticipantService(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public MyTeamResponse getMyTeam(UUID participantId) {
        TeamMember myMembership = teamMemberRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new NotFoundException(
                        "Aucune equipe attribuee pour l'instant: le matchmaking n'a pas encore ete lance."));

        List<TeammateResponse> partners = teamMemberRepository.findByTeamId(myMembership.getTeam().getId()).stream()
                .filter(member -> !member.getParticipant().getId().equals(participantId))
                .map(TeammateResponse::from)
                .toList();

        var myCharacter = myMembership.getCharacter();
        return new MyTeamResponse(
                myMembership.getTeam().getId(),
                myCharacter != null ? myCharacter.getId() : null,
                myCharacter != null ? myCharacter.getName() : null,
                myCharacter != null ? myCharacter.getAvatarUrl() : null,
                myCharacter != null ? myCharacter.getDescription() : null,
                partners);
    }
}
