package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.blindtest.dto.TrackCreateRequest;
import com.weddinggames.backend.blindtest.dto.TrackResponse;
import com.weddinggames.backend.blindtest.dto.TrackUpdateRequest;
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
@Tag(name = "Admin - Blind test", description = "Catalogue des morceaux d'une partie de blind test (reserve a l'administrateur)")
public class TrackAdminController {

    private final TrackAdminService trackAdminService;

    public TrackAdminController(TrackAdminService trackAdminService) {
        this.trackAdminService = trackAdminService;
    }

    @GetMapping("/games/{gameId}/tracks")
    public List<TrackResponse> list(@PathVariable UUID gameId) {
        return trackAdminService.listByGame(gameId).stream().map(TrackResponse::from).toList();
    }

    @PostMapping("/games/{gameId}/tracks")
    public ResponseEntity<TrackResponse> create(
            @PathVariable UUID gameId, @Valid @RequestBody TrackCreateRequest request) {
        Track created = trackAdminService.create(gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TrackResponse.from(created));
    }

    @GetMapping("/tracks/{id}")
    public TrackResponse get(@PathVariable UUID id) {
        return TrackResponse.from(trackAdminService.get(id));
    }

    @PutMapping("/tracks/{id}")
    public TrackResponse update(@PathVariable UUID id, @Valid @RequestBody TrackUpdateRequest request) {
        return TrackResponse.from(trackAdminService.update(id, request));
    }

    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        trackAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
