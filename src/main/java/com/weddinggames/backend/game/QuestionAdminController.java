package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.QuestionCreateRequest;
import com.weddinggames.backend.game.dto.QuestionResponse;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Questions", description = "Configuration des questions d'une partie (reserve a l'administrateur)")
public class QuestionAdminController {

    private final QuestionService questionService;

    public QuestionAdminController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/games/{gameId}/questions")
    public List<QuestionResponse> list(@PathVariable UUID gameId) {
        return questionService.listByGame(gameId).stream().map(QuestionResponse::from).toList();
    }

    @PostMapping("/games/{gameId}/questions")
    public ResponseEntity<QuestionResponse> create(
            @PathVariable UUID gameId, @Valid @RequestBody QuestionCreateRequest request) {
        Question created = questionService.create(gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionResponse.from(created));
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse get(@PathVariable UUID id) {
        return QuestionResponse.from(questionService.get(id));
    }
}
