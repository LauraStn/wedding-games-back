package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.whosaidit.dto.WhoSaidItQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/events/{eventId}/who-said-it/questions")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Selection Who Said It", description = "Tirage aleatoire d'une question acceptee pour le jeu")
public class WhoSaidItSelectionController {

    private final WhoSaidItSelectionService selectionService;

    public WhoSaidItSelectionController(WhoSaidItSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @PostMapping("/select-random")
    @Operation(
            summary = "Tire au hasard une question acceptee et la fait passer a jouee",
            description = "Echoue explicitement (409) si aucune question acceptee n'est disponible. Le prenom de "
                    + "l'auteur n'est renvoye que s'il a consenti a etre revele: c'est le moment ou ce "
                    + "consentement compte le plus, contrairement a la moderation ou le staff voit toujours "
                    + "qui a propose quoi.")
    public WhoSaidItQuestionResponse selectRandom(@PathVariable UUID eventId) {
        return WhoSaidItQuestionResponse.from(selectionService.selectRandom(eventId));
    }
}
