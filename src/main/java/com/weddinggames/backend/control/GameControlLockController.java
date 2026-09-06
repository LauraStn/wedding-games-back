package com.weddinggames.backend.control;

import com.weddinggames.backend.control.dto.GameControlLockResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
import com.weddinggames.backend.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/games/{gameId}/control-lock")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Verrou de controle", description = "Un seul intervenant pilote une partie a la fois")
public class GameControlLockController {

    private final GameControlLockService lockService;

    public GameControlLockController(GameControlLockService lockService) {
        this.lockService = lockService;
    }

    @GetMapping
    @Operation(summary = "Consulte qui pilote actuellement cette partie, le cas echeant")
    public GameControlLockResponse get(@PathVariable UUID gameId) {
        return GameControlLockResponse.from(lockService.getOrCreate(gameId));
    }

    @PostMapping("/claim")
    @Operation(
            summary = "Prend le controle de la partie",
            description = "Echoue explicitement (409) si un autre intervenant le detient deja.")
    public GameControlLockResponse claim(@PathVariable UUID gameId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return GameControlLockResponse.from(lockService.claim(gameId, actor.staffAccountId()));
    }

    @PostMapping("/release")
    @Operation(
            summary = "Relache le controle de la partie",
            description = "Un ADMIN peut toujours relacher un verrou bloque, meme detenu par quelqu'un d'autre.")
    public GameControlLockResponse release(
            @PathVariable UUID gameId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return GameControlLockResponse.from(
                lockService.release(gameId, actor.staffAccountId(), actor.role() == Role.ADMIN));
    }
}
