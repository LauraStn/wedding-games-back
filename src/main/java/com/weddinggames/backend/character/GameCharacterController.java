package com.weddinggames.backend.character;

import com.weddinggames.backend.character.dto.GameCharacterCreateRequest;
import com.weddinggames.backend.character.dto.GameCharacterResponse;
import com.weddinggames.backend.character.dto.GameCharacterUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Personnages", description = "Catalogue de personnages (reserve a l'administrateur)")
public class GameCharacterController {

    private final GameCharacterService gameCharacterService;

    public GameCharacterController(GameCharacterService gameCharacterService) {
        this.gameCharacterService = gameCharacterService;
    }

    @GetMapping("/events/{eventId}/characters")
    public List<GameCharacterResponse> list(@PathVariable UUID eventId) {
        return gameCharacterService.listByEvent(eventId).stream()
                .map(GameCharacterResponse::from)
                .toList();
    }

    @PostMapping("/events/{eventId}/characters")
    public ResponseEntity<GameCharacterResponse> create(
            @PathVariable UUID eventId, @Valid @RequestBody GameCharacterCreateRequest request) {
        GameCharacter created = gameCharacterService.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GameCharacterResponse.from(created));
    }

    @GetMapping("/characters/{id}")
    public GameCharacterResponse get(@PathVariable UUID id) {
        return GameCharacterResponse.from(gameCharacterService.get(id));
    }

    @PutMapping("/characters/{id}")
    public GameCharacterResponse update(@PathVariable UUID id, @Valid @RequestBody GameCharacterUpdateRequest request) {
        return GameCharacterResponse.from(gameCharacterService.update(id, request));
    }

    @PostMapping("/characters/{id}/activate")
    public GameCharacterResponse activate(@PathVariable UUID id) {
        return GameCharacterResponse.from(gameCharacterService.activate(id));
    }

    @PostMapping("/characters/{id}/deactivate")
    public GameCharacterResponse deactivate(@PathVariable UUID id) {
        return GameCharacterResponse.from(gameCharacterService.deactivate(id));
    }

    @DeleteMapping("/characters/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        gameCharacterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
