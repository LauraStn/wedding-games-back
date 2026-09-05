package com.weddinggames.backend.vote;

import com.weddinggames.backend.security.AuthenticatedActor;
import com.weddinggames.backend.vote.dto.VoteCastRequest;
import com.weddinggames.backend.vote.dto.VoteResponse;
import com.weddinggames.backend.vote.dto.VotingOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vote/questions/{questionId}")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Vote", description = "Vote public sur les reponses acceptees d'une question, anonymise et sans auto-vote")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @GetMapping("/options")
    @Operation(
            summary = "Options de vote pour cette question",
            description = "Reponses acceptees uniquement, ordre aleatoire, jamais la reponse de ma propre "
                    + "equipe, sans nom ni personnage visible.")
    public List<VotingOptionResponse> options(
            @PathVariable UUID questionId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return voteService.listBallot(questionId, actor.participantId()).stream()
                .map(VotingOptionResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Vote pour une reponse (jamais celle de sa propre equipe, une seule fois par question)")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @Valid @RequestBody VoteCastRequest request) {
        var vote = voteService.castVote(questionId, actor.participantId(), request.answerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VoteResponse.from(vote));
    }
}
