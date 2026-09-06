package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.blindtest.dto.TrackStateResponse;
import com.weddinggames.backend.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Intervenant - Blind test", description = "Pilotage des manches (activation, chronometre) et consultation pour la projection")
public class TrackStaffController {

    private final TrackStaffService trackStaffService;

    public TrackStaffController(TrackStaffService trackStaffService) {
        this.trackStaffService = trackStaffService;
    }

    @GetMapping("/games/{gameId}/tracks/active")
    @PreAuthorize("hasAnyRole('INTERVENANT','PROJECTION','ADMIN')")
    @Operation(summary = "Consulte le morceau actuellement actif (et son chronometre), pour la projection")
    public TrackStateResponse getActive(@PathVariable UUID gameId) {
        Track track = trackStaffService
                .getCurrentActive(gameId)
                .orElseThrow(() -> new NotFoundException("Aucun morceau actif pour cette partie."));
        return TrackStateResponse.from(track, trackStaffService.remainingSeconds(track));
    }

    @GetMapping("/tracks/{trackId}/state")
    @PreAuthorize("hasAnyRole('INTERVENANT','PROJECTION','ADMIN')")
    @Operation(summary = "Consulte l'etat (statut, chronometre) d'un morceau")
    public TrackStateResponse getState(@PathVariable UUID trackId) {
        Track track = trackStaffService.get(trackId);
        return TrackStateResponse.from(track, trackStaffService.remainingSeconds(track));
    }

    @PostMapping("/tracks/{trackId}/activate")
    @Operation(summary = "Active le morceau (PENDING -> ACTIVE): devient la manche en cours")
    public TrackStateResponse activate(@PathVariable UUID trackId) {
        Track track = trackStaffService.activate(trackId);
        return TrackStateResponse.from(track, trackStaffService.remainingSeconds(track));
    }

    @PostMapping("/tracks/{trackId}/close")
    @Operation(summary = "Ferme le morceau (ACTIVE -> CLOSED): la manche est terminee")
    public TrackStateResponse close(@PathVariable UUID trackId) {
        Track track = trackStaffService.close(trackId);
        return TrackStateResponse.from(track, trackStaffService.remainingSeconds(track));
    }

    @PostMapping("/tracks/{trackId}/start-timer")
    @Operation(summary = "Lance le chronometre du morceau actif, dure le nombre de secondes configure")
    public TrackStateResponse startTimer(@PathVariable UUID trackId) {
        Track track = trackStaffService.startTimer(trackId);
        return TrackStateResponse.from(track, trackStaffService.remainingSeconds(track));
    }
}
