package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.luiouelle.dto.LuiOuElleCorrectionRequest;
import com.weddinggames.backend.luiouelle.dto.LuiOuElleQuestionResponse;
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
@Tag(name = "Intervenant - Moderation Lui ou Elle", description = "Acceptation, refus et correction des questions proposees par les invites")
public class LuiOuElleModerationController {

    private final LuiOuElleModerationService moderationService;

    public LuiOuElleModerationController(LuiOuElleModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/api/v1/staff/events/{eventId}/lui-ou-elle/questions")
    @Operation(summary = "Liste toutes les questions proposees pour un evenement, pour moderation")
    public List<LuiOuElleQuestionResponse> list(@PathVariable UUID eventId) {
        return moderationService.listForEvent(eventId).stream()
                .map(LuiOuElleQuestionResponse::forStaff)
                .toList();
    }

    @PostMapping("/api/v1/staff/lui-ou-elle/questions/{id}/accept")
    @Operation(summary = "Accepte la question: eligible a la selection aleatoire pour le jeu")
    public LuiOuElleQuestionResponse accept(@PathVariable UUID id) {
        return LuiOuElleQuestionResponse.forStaff(moderationService.accept(id));
    }

    @PostMapping("/api/v1/staff/lui-ou-elle/questions/{id}/reject")
    @Operation(summary = "Refuse la question: jamais selectionnee pour le jeu")
    public LuiOuElleQuestionResponse reject(@PathVariable UUID id) {
        return LuiOuElleQuestionResponse.forStaff(moderationService.reject(id));
    }

    @PutMapping("/api/v1/staff/lui-ou-elle/questions/{id}/content")
    @Operation(summary = "Corrige le contenu (faute de frappe, texte illisible) sans changer le sens")
    public LuiOuElleQuestionResponse correct(
            @PathVariable UUID id, @Valid @RequestBody LuiOuElleCorrectionRequest request) {
        return LuiOuElleQuestionResponse.forStaff(moderationService.correct(id, request.content()));
    }
}
