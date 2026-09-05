package com.weddinggames.backend.vote;

import com.weddinggames.backend.vote.dto.FinalistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/questions/{questionId}/finalists")
@PreAuthorize("hasAnyRole('JURY','INTERVENANT','ADMIN')")
@Tag(name = "Jury - Finalistes", description = "Top 3 des reponses les plus votees (egalites incluses), transmis au jury")
public class FinalistController {

    private final FinalistService finalistService;

    public FinalistController(FinalistService finalistService) {
        this.finalistService = finalistService;
    }

    @GetMapping
    @Operation(
            summary = "Finalistes du top 3 (jusqu'a 4+ en cas d'egalite, jamais de tirage au sort eliminant une egalite)",
            description = "Le nombre de votes est masque par defaut (revealVoteCount=false) pour ne pas "
                    + "influencer le jugement du jury; passer revealVoteCount=true pour l'afficher.")
    public List<FinalistResponse> finalists(
            @PathVariable UUID questionId,
            @RequestParam(defaultValue = "false") boolean revealVoteCount) {
        return finalistService.computeFinalists(questionId).stream()
                .map(finalist -> FinalistResponse.from(finalist, revealVoteCount))
                .toList();
    }
}
