package com.weddinggames.backend.quiz;

import com.weddinggames.backend.quiz.dto.QuizAnswerResponse;
import com.weddinggames.backend.quiz.dto.QuizAnswerUpdateRequest;
import com.weddinggames.backend.security.AuthenticatedActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/api/v1/quiz/questions/{questionId}/answer")
@PreAuthorize("hasRole('PARTICIPANT')")
@Tag(name = "Quiz", description = "Saisie d'equipe en direct: prise de main, transfert du controle, reponse synchronisee")
public class QuizAnswerController {

    private final QuizAnswerService quizAnswerService;

    public QuizAnswerController(QuizAnswerService quizAnswerService) {
        this.quizAnswerService = quizAnswerService;
    }

    @GetMapping
    @Operation(summary = "Etat actuel de la reponse de mon equipe (poll pour suivre la saisie en direct)")
    public QuizAnswerResponse getMyTeamAnswer(
            @PathVariable UUID questionId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return QuizAnswerResponse.from(quizAnswerService.getMyTeamAnswer(questionId, actor.participantId()));
    }

    @PostMapping("/take-control")
    @Operation(
            summary = "Je prends la main: je deviens redacteur pour mon equipe",
            description = "Peut aussi etre appele pour transferer le controle: le precedent redacteur "
                    + "passe en lecture seule, le contenu deja saisi est conserve.")
    public QuizAnswerResponse takeControl(
            @PathVariable UUID questionId, @AuthenticationPrincipal AuthenticatedActor actor) {
        return QuizAnswerResponse.from(quizAnswerService.takeControl(questionId, actor.participantId()));
    }

    @PutMapping
    @Operation(summary = "Met a jour le contenu de la reponse (reserve a celui qui a la main)")
    public QuizAnswerResponse updateContent(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal AuthenticatedActor actor,
            @Valid @RequestBody QuizAnswerUpdateRequest request) {
        return QuizAnswerResponse.from(
                quizAnswerService.updateContent(questionId, actor.participantId(), request.content()));
    }
}
