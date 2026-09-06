package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.luiouelle.dto.LuiOuElleQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/events/{eventId}/lui-ou-elle/questions")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Selection Lui ou Elle", description = "Tirage aleatoire d'une question acceptee pour le jeu")
public class LuiOuElleSelectionController {

    private final LuiOuElleSelectionService selectionService;

    public LuiOuElleSelectionController(LuiOuElleSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @PostMapping("/select-random")
    @Operation(
            summary = "Tire au hasard une question acceptee et la fait passer a jouee",
            description = "Echoue explicitement (409) si aucune question acceptee n'est disponible. Le prenom de "
                    + "l'auteur n'est renvoye que s'il a consenti a etre revele: c'est le moment ou ce "
                    + "consentement compte le plus, contrairement a la moderation ou le staff voit toujours "
                    + "qui a propose quoi.")
    public LuiOuElleQuestionResponse selectRandom(@PathVariable UUID eventId) {
        return LuiOuElleQuestionResponse.from(selectionService.selectRandom(eventId));
    }
}
