package com.weddinggames.backend.lobby;

import com.weddinggames.backend.lobby.dto.LobbyParticipantResponse;
import com.weddinggames.backend.lobby.dto.LobbyResponse;
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
@RequestMapping("/api/v1/staff/events/{eventId}/lobby")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Salon", description = "Pilotage du salon d'attente (ouverture, fermeture, arrivees)")
public class LobbyStaffController {

    private final LobbyService lobbyService;

    public LobbyStaffController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("/open")
    public LobbyResponse open(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.open(eventId));
    }

    @PostMapping("/close")
    public LobbyResponse close(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.close(eventId));
    }

    @PostMapping("/lock")
    public LobbyResponse lock(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.lock(eventId));
    }

    @PostMapping("/start")
    public LobbyResponse start(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.start(eventId));
    }

    @PostMapping("/pause")
    public LobbyResponse pause(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.pause(eventId));
    }

    @PostMapping("/resume")
    public LobbyResponse resume(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.resume(eventId));
    }

    @PostMapping("/finish")
    public LobbyResponse finish(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.finish(eventId));
    }

    @GetMapping("/participants")
    public List<LobbyParticipantResponse> participants(@PathVariable UUID eventId) {
        return lobbyService.listParticipants(eventId).stream()
                .map(LobbyParticipantResponse::from)
                .toList();
    }

    @PostMapping("/participants/{participantId}/late")
    public LobbyParticipantResponse markLate(@PathVariable UUID eventId, @PathVariable UUID participantId) {
        return LobbyParticipantResponse.from(lobbyService.markLate(eventId, participantId));
    }

    @PostMapping("/participants/{participantId}/admit")
    public LobbyParticipantResponse admit(@PathVariable UUID eventId, @PathVariable UUID participantId) {
        return LobbyParticipantResponse.from(lobbyService.admit(eventId, participantId));
    }
}
