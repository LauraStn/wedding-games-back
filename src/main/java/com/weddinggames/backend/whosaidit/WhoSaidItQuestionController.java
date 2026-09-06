package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.whosaidit.dto.WhoSaidItQuestionRequest;
import com.weddinggames.backend.whosaidit.dto.WhoSaidItQuestionResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/who-said-it/questions")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Who Said It - Propositions", description = "Proposition et modification de questions par les invites, tant que le salon reste ouvert")
public class WhoSaidItQuestionController {

    private final WhoSaidItQuestionService questionService;

    public WhoSaidItQuestionController(WhoSaidItQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/me")
    @Operation(summary = "Liste mes questions proposees")
    public List<WhoSaidItQuestionResponse> mine(@AuthenticationPrincipal AuthenticatedActor actor) {
        return questionService.listMine(actor.participantId()).stream()
                .map(WhoSaidItQuestionResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Propose une nouvelle question (limite au nombre de questions par participant)")
    public ResponseEntity<WhoSaidItQuestionResponse> propose(
            @AuthenticationPrincipal AuthenticatedActor actor, @Valid @RequestBody WhoSaidItQuestionRequest request) {
        var question = questionService.propose(actor.participantId(), request.content(), request.revealAuthorConsent());
        return ResponseEntity.status(HttpStatus.CREATED).body(WhoSaidItQuestionResponse.from(question));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie une de mes questions, tant que le salon reste ouvert")
    public WhoSaidItQuestionResponse update(
            @AuthenticationPrincipal AuthenticatedActor actor,
            @PathVariable UUID id,
            @Valid @RequestBody WhoSaidItQuestionRequest request) {
        return WhoSaidItQuestionResponse.from(
                questionService.update(actor.participantId(), id, request.content(), request.revealAuthorConsent()));
    }
}
