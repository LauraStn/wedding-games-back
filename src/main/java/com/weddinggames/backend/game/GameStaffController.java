package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.GameResponse;
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
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Parties", description = "Consultation et pilotage de la machine d'etat commune aux jeux")
public class GameStaffController {

    private final GameService gameService;

    public GameStaffController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/events/{eventId}/games")
    @Operation(summary = "Liste les parties d'un evenement, pour que l'intervenant choisisse laquelle piloter")
    public List<GameResponse> list(@PathVariable UUID eventId) {
        return gameService.listByEvent(eventId).stream().map(GameResponse::from).toList();
    }

    @GetMapping("/games/{gameId}")
    @Operation(summary = "Consulte une partie")
    public GameResponse get(@PathVariable UUID gameId) {
        return GameResponse.from(gameService.get(gameId));
    }

    @PostMapping("/games/{gameId}/start")
    @Operation(summary = "Demarre la partie (statut ACTIVE, phase PREPARATION)")
    public GameResponse start(@PathVariable UUID gameId) {
        return GameResponse.from(gameService.start(gameId));
    }

    @PostMapping("/games/{gameId}/pause")
    @Operation(summary = "Met la partie en pause, sans changer la phase en cours")
    public GameResponse pause(@PathVariable UUID gameId) {
        return GameResponse.from(gameService.pause(gameId));
    }

    @PostMapping("/games/{gameId}/resume")
    @Operation(summary = "Reprend une partie en pause, sur la meme phase")
    public GameResponse resume(@PathVariable UUID gameId) {
        return GameResponse.from(gameService.resume(gameId));
    }

    @PostMapping("/games/{gameId}/next-question")
    @Operation(summary = "Passe a la question suivante (phase QUESTION), depuis PREPARATION ou RESULT")
    public GameResponse nextQuestion(@PathVariable UUID gameId) {
        return GameResponse.from(gameService.nextQuestion(gameId));
    }
}
