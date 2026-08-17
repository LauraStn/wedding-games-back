package com.weddinggames.backend.lobby;

import com.weddinggames.backend.lobby.dto.LobbyParticipantResponse;
import com.weddinggames.backend.lobby.dto.LobbyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/lobby")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Salon", description = "Consultation du salon d'attente")
public class LobbyAdminController {

    private final LobbyService lobbyService;

    public LobbyAdminController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping
    public LobbyResponse getLobby(@PathVariable UUID eventId) {
        return LobbyResponse.from(lobbyService.getOrCreate(eventId));
    }

    @GetMapping("/participants")
    public List<LobbyParticipantResponse> participants(@PathVariable UUID eventId) {
        return lobbyService.listParticipants(eventId).stream()
                .map(LobbyParticipantResponse::from)
                .toList();
    }
}
