package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.whosaidit.dto.WhoSaidItCorrectionRequest;
import com.weddinggames.backend.whosaidit.dto.WhoSaidItQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Moderation Who Said It", description = "Acceptation, refus et correction des questions proposees par les invites")
public class WhoSaidItModerationController {

    private final WhoSaidItModerationService moderationService;

    public WhoSaidItModerationController(WhoSaidItModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/api/v1/staff/events/{eventId}/who-said-it/questions")
    @Operation(summary = "Liste toutes les questions proposees pour un evenement, pour moderation")
    public List<WhoSaidItQuestionResponse> list(@PathVariable UUID eventId) {
        return moderationService.listForEvent(eventId).stream()
                .map(WhoSaidItQuestionResponse::forStaff)
                .toList();
    }

    @PostMapping("/api/v1/staff/who-said-it/questions/{id}/accept")
    @Operation(summary = "Accepte la question: eligible a la selection aleatoire pour le jeu")
    public WhoSaidItQuestionResponse accept(@PathVariable UUID id) {
        return WhoSaidItQuestionResponse.forStaff(moderationService.accept(id));
    }

    @PostMapping("/api/v1/staff/who-said-it/questions/{id}/reject")
    @Operation(summary = "Refuse la question: jamais selectionnee pour le jeu")
    public WhoSaidItQuestionResponse reject(@PathVariable UUID id) {
        return WhoSaidItQuestionResponse.forStaff(moderationService.reject(id));
    }

    @PutMapping("/api/v1/staff/who-said-it/questions/{id}/content")
    @Operation(summary = "Corrige le contenu (faute de frappe, texte illisible) sans changer le sens")
    public WhoSaidItQuestionResponse correct(
            @PathVariable UUID id, @Valid @RequestBody WhoSaidItCorrectionRequest request) {
        return WhoSaidItQuestionResponse.forStaff(moderationService.correct(id, request.content()));
    }
}
