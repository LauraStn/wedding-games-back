package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.CurrentGameResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games/current")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Jeu en cours", description = "Ce que la salle joue maintenant, pour l'ecran de l'invite (a poller)")
public class CurrentGameController {

    private final CurrentGameService currentGameService;

    public CurrentGameController(CurrentGameService currentGameService) {
        this.currentGameService = currentGameService;
    }

    @GetMapping
    @Operation(
            summary = "Jeu actif et question courante pour l'evenement du participant",
            description = "game et question valent null quand rien n'est lance; question seule vaut null "
                    + "tant que la partie est en preparation. Jamais 404.")
    public CurrentGameResponse current(@AuthenticationPrincipal AuthenticatedActor actor) {
        return CurrentGameResponse.from(currentGameService.currentGameFor(actor.participantId()));
    }
}
