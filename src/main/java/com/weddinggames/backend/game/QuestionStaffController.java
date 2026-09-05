package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Questions", description = "Consultation, activation et fermeture d'une question")
public class QuestionStaffController {

    private final QuestionService questionService;

    public QuestionStaffController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/games/{gameId}/questions")
    @Operation(summary = "Liste les questions d'une partie, pour que l'intervenant choisisse laquelle piloter")
    public List<QuestionResponse> list(@PathVariable UUID gameId) {
        return questionService.listByGame(gameId).stream().map(QuestionResponse::from).toList();
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Consulte une question")
    public QuestionResponse get(@PathVariable UUID questionId) {
        return QuestionResponse.from(questionService.get(questionId));
    }

    @PostMapping("/questions/{questionId}/activate")
    @Operation(summary = "Active la question (PENDING -> ACTIVE): les equipes peuvent desormais y repondre")
    public QuestionResponse activate(@PathVariable UUID questionId) {
        return QuestionResponse.from(questionService.activate(questionId));
    }

    @PostMapping("/questions/{questionId}/close")
    @Operation(summary = "Ferme la question (ACTIVE -> CLOSED): plus aucune modification de reponse n'est acceptee")
    public QuestionResponse close(@PathVariable UUID questionId) {
        return QuestionResponse.from(questionService.close(questionId));
    }
}
