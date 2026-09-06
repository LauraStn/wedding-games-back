package com.weddinggames.backend.projection;

import com.weddinggames.backend.projection.dto.ProjectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/events/{eventId}/projection")
@PreAuthorize("hasAnyRole('PROJECTION','INTERVENANT','ADMIN')")
@Tag(name = "Projection", description = "Etat agrege en lecture seule pour l'ecran de projection")
public class ProjectionController {

    private final ProjectionService projectionService;

    public ProjectionController(ProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping
    @Operation(
            summary = "Etat agrege temps reel: salon, jeu actif, chronometre, reponses anonymes, top 3, podium",
            description = "Strictement en lecture: aucune mutation n'est possible via ce endpoint ou ce role.")
    public ProjectionResponse get(@PathVariable UUID eventId) {
        return projectionService.get(eventId);
    }
}
