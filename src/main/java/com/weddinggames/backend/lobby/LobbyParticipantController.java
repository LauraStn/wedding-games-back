package com.weddinggames.backend.lobby;

import com.weddinggames.backend.lobby.dto.LobbyHeartbeatResponse;
import com.weddinggames.backend.lobby.dto.LobbyParticipantStatusResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    public LobbyHeartbeatResponse heartbeat(@AuthenticationPrincipal AuthenticatedActor actor) {
        return LobbyHeartbeatResponse.from(lobbyService.heartbeat(actor.participantId()));
    }

    @PostMapping("/ready")
    @Operation(summary = "Declare le participant courant pret, distinct d'une simple presence connectee")
    public LobbyHeartbeatResponse ready(@AuthenticationPrincipal AuthenticatedActor actor) {
        return LobbyHeartbeatResponse.from(lobbyService.markReady(actor.participantId()));
    }

    @GetMapping
    @Operation(summary = "Etat du salon adapte au participant (statut, nombre de presents, consignes)")
    public LobbyParticipantStatusResponse getStatus(@AuthenticationPrincipal AuthenticatedActor actor) {
        return lobbyService.getStatusForParticipant(actor.participantId());
    }
}
