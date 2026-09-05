package com.weddinggames.backend.game;

import com.weddinggames.backend.game.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/questions/{questionId}")
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Questions", description = "Activation et fermeture d'une question")
public class QuestionStaffController {

    private final QuestionService questionService;

    public QuestionStaffController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/activate")
    @Operation(summary = "Active la question (PENDING -> ACTIVE): les equipes peuvent desormais y repondre")
    public QuestionResponse activate(@PathVariable UUID questionId) {
        return QuestionResponse.from(questionService.activate(questionId));
    }

    @PostMapping("/close")
    @Operation(summary = "Ferme la question (ACTIVE -> CLOSED): plus aucune modification de reponse n'est acceptee")
    public QuestionResponse close(@PathVariable UUID questionId) {
        return QuestionResponse.from(questionService.close(questionId));
    }
}
