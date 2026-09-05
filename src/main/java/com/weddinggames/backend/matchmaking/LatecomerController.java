package com.weddinggames.backend.matchmaking;

import com.weddinggames.backend.matchmaking.dto.LatecomerOptionsResponse;
import com.weddinggames.backend.matchmaking.dto.TeamResponse;
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
@RequestMapping("/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Retardataires", description = "Integration manuelle d'un retardataire dans une equipe compatible")
public class LatecomerController {

    private final LatecomerIntegrationService latecomerIntegrationService;

    public LatecomerController(LatecomerIntegrationService latecomerIntegrationService) {
        this.latecomerIntegrationService = latecomerIntegrationService;
    }

    @GetMapping("/options")
    @Operation(
            summary = "Equipes compatibles (deviendraient un trio) et autres retardataires compatibles "
                    + "(formeraient un nouveau binome), en respectant les exclusions absolues")
    public LatecomerOptionsResponse getOptions(@PathVariable UUID eventId, @PathVariable UUID participantId) {
        return latecomerIntegrationService.getOptions(eventId, participantId);
    }

    @PostMapping("/join-team/{teamId}")
    @Operation(summary = "Integre le retardataire dans un binome existant, qui devient un trio")
    public TeamResponse joinTeam(
            @PathVariable UUID eventId, @PathVariable UUID participantId, @PathVariable UUID teamId) {
        return latecomerIntegrationService.joinExistingTeam(eventId, participantId, teamId);
    }

    @PostMapping("/pair-with/{otherParticipantId}")
    @Operation(summary = "Forme un nouveau binome entre deux retardataires")
    public TeamResponse pairWith(
            @PathVariable UUID eventId, @PathVariable UUID participantId, @PathVariable UUID otherParticipantId) {
        return latecomerIntegrationService.pairTwoLatecomers(eventId, participantId, otherParticipantId);
    }
}
