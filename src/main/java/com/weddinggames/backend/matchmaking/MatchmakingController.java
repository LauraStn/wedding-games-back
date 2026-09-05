package com.weddinggames.backend.matchmaking;

import com.weddinggames.backend.matchmaking.dto.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/events/{eventId}/matchmaking")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Matchmaking", description = "Generation des equipes a partir des participants presents")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/launch")
    @Operation(
            summary = "Genere les equipes a partir des participants presents dans le salon",
            description = "Peut etre relance a volonte: les equipes precedentes sont remplacees. Echoue "
                    + "explicitement (409) si aucune repartition ne respecte les exclusions absolues.")
    public List<TeamResponse> launch(@PathVariable UUID eventId) {
        return matchmakingService.launch(eventId);
    }

    @GetMapping("/teams")
    @Operation(summary = "Consulte les equipes actuelles sans relancer le matchmaking")
    public List<TeamResponse> teams(@PathVariable UUID eventId) {
        return matchmakingService.listTeams(eventId);
    }
}
