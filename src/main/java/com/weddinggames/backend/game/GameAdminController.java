package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.GameCreateRequest;
import com.weddinggames.backend.game.dto.GameResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Parties", description = "Configuration des parties (reserve a l'administrateur)")
public class GameAdminController {

    private final GameService gameService;

    public GameAdminController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/events/{eventId}/games")
    public List<GameResponse> list(@PathVariable UUID eventId) {
        return gameService.listByEvent(eventId).stream().map(GameResponse::from).toList();
    }

    @PostMapping("/events/{eventId}/games")
    public ResponseEntity<GameResponse> create(
            @PathVariable UUID eventId, @Valid @RequestBody GameCreateRequest request) {
        Game created = gameService.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GameResponse.from(created));
    }

    @GetMapping("/games/{id}")
    public GameResponse get(@PathVariable UUID id) {
        return GameResponse.from(gameService.get(id));
    }
}
