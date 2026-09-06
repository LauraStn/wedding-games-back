package com.weddinggames.backend.score;

import com.weddinggames.backend.score.dto.PodiumEntryResponse;
import com.weddinggames.backend.score.dto.ScoreAwardRequest;
import com.weddinggames.backend.score.dto.ScoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/events/{eventId}")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Scores", description = "Attribution des points aux equipes et calcul du podium")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/scores")
    @Operation(summary = "Attribue (ou retire) des points a une equipe; le bareme est fourni par l'appelant")
    public ResponseEntity<ScoreResponse> award(
            @PathVariable UUID eventId, @Valid @RequestBody ScoreAwardRequest request) {
        var score = scoreService.award(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScoreResponse.from(score));
    }

    @GetMapping("/scores")
    @Operation(summary = "Liste le journal des points attribues pour l'evenement")
    public List<ScoreResponse> list(@PathVariable UUID eventId) {
        return scoreService.listByEvent(eventId).stream().map(ScoreResponse::from).toList();
    }

    @GetMapping("/podium")
    @PreAuthorize("hasAnyRole('INTERVENANT','JURY','PROJECTION','ADMIN')")
    @Operation(summary = "Classement des equipes par total de points (egalites partagees, jamais de tirage au sort)")
    public List<PodiumEntryResponse> podium(@PathVariable UUID eventId) {
        return scoreService.podium(eventId);
    }
}
