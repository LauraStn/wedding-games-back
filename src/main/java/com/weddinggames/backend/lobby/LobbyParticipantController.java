package com.weddinggames.backend.lobby;

import com.weddinggames.backend.lobby.dto.LobbyParticipantResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lobby")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Salon", description = "Heartbeat de presence du participant dans le salon d'attente")
public class LobbyParticipantController {

    private final LobbyService lobbyService;

    public LobbyParticipantController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Signale la presence du participant courant dans le salon (a appeler periodiquement)")
    public LobbyParticipantResponse heartbeat(@AuthenticationPrincipal AuthenticatedActor actor) {
        return LobbyParticipantResponse.from(lobbyService.heartbeat(actor.participantId()));
    }
}
