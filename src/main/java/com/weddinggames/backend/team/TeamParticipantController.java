package com.weddinggames.backend.team;

import com.weddinggames.backend.security.AuthenticatedActor;
import com.weddinggames.backend.team.dto.MyTeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/team")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Equipe", description = "Decouverte du personnage assigne et du binome/trio, une fois le matchmaking effectue")
public class TeamParticipantController {

    private final TeamParticipantService teamParticipantService;

    public TeamParticipantController(TeamParticipantService teamParticipantService) {
        this.teamParticipantService = teamParticipantService;
    }

    @GetMapping("/me")
    @Operation(summary = "Mon personnage assigne et celui de mon/mes partenaire(s) de binome/trio")
    public MyTeamResponse getMyTeam(@AuthenticationPrincipal AuthenticatedActor actor) {
        return teamParticipantService.getMyTeam(actor.participantId());
    }
}
