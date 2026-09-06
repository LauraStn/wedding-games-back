package com.weddinggames.backend.jury;

import com.weddinggames.backend.jury.dto.JuryChooseRequest;
import com.weddinggames.backend.jury.dto.JuryDecisionResponse;
import com.weddinggames.backend.jury.dto.JuryPointsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/questions/{questionId}/jury-decision")
@Tag(name = "Jury - Decision", description = "Choix, confirmation, bonus et revelation de l'equipe gagnante")
public class JuryController {

    private final JuryService juryService;

    public JuryController(JuryService juryService) {
        this.juryService = juryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('JURY','INTERVENANT','ADMIN')")
    @Operation(summary = "Consulte l'etat de la decision du jury pour cette question")
    public JuryDecisionResponse get(@PathVariable UUID questionId) {
        return JuryDecisionResponse.from(juryService.getOrCreate(questionId));
    }

    @PostMapping("/choose")
    @PreAuthorize("hasAnyRole('JURY','ADMIN')")
    @Operation(
            summary = "Choisit (ou change, tant que non confirme) la reponse gagnante parmi les finalistes",
            description = "Refuse (409) une reponse qui ne fait pas partie des finalistes de la question.")
    public JuryDecisionResponse choose(
            @PathVariable UUID questionId, @Valid @RequestBody JuryChooseRequest request) {
        return JuryDecisionResponse.from(juryService.choose(questionId, request.answerId()));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('JURY','ADMIN')")
    @Operation(
            summary = "Confirme le choix (definitif) et attribue les points de la manche a l'equipe gagnante",
            description = "Refuse (409) tant qu'aucune reponse n'a ete choisie.")
    public JuryDecisionResponse confirm(
            @PathVariable UUID questionId, @RequestBody JuryPointsRequest request) {
        return JuryDecisionResponse.from(juryService.confirm(questionId, request));
    }

    @PostMapping("/bonus")
    @PreAuthorize("hasAnyRole('JURY','ADMIN')")
    @Operation(
            summary = "Attribue un bonus optionnel a l'equipe gagnante",
            description = "Refuse (409) tant que la decision n'est pas confirmee.")
    public JuryDecisionResponse bonus(
            @PathVariable UUID questionId, @RequestBody JuryPointsRequest request) {
        return JuryDecisionResponse.from(juryService.bonus(questionId, request));
    }

    @PostMapping("/reveal")
    @PreAuthorize("hasAnyRole('JURY','ADMIN')")
    @Operation(
            summary = "Revele l'equipe gagnante",
            description = "Refuse (409) tant que la decision n'est pas confirmee.")
    public JuryDecisionResponse reveal(@PathVariable UUID questionId) {
        return JuryDecisionResponse.from(juryService.reveal(questionId));
    }
}
