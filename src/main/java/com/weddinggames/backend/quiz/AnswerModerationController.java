package com.weddinggames.backend.quiz;

import com.weddinggames.backend.quiz.dto.AnswerCorrectionRequest;
import com.weddinggames.backend.quiz.dto.AnswerModerationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('INTERVENANT','ADMIN')")
@Tag(name = "Intervenant - Moderation quiz", description = "Moderation des reponses avant projection, vote et jury")
public class AnswerModerationController {

    private final AnswerModerationService answerModerationService;

    public AnswerModerationController(AnswerModerationService answerModerationService) {
        this.answerModerationService = answerModerationService;
    }

    @GetMapping("/api/v1/staff/questions/{questionId}/answers")
    @Operation(summary = "Liste toutes les reponses d'une question pour moderation (y compris masquees)")
    public List<AnswerModerationResponse> list(@PathVariable UUID questionId) {
        return answerModerationService.listForQuestion(questionId).stream()
                .map(AnswerModerationResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/staff/answers/{answerId}/accept")
    @Operation(summary = "Accepte la reponse: eligible a la projection, au vote et au jury")
    public AnswerModerationResponse accept(@PathVariable UUID answerId) {
        return AnswerModerationResponse.from(answerModerationService.accept(answerId));
    }

    @PostMapping("/api/v1/staff/answers/{answerId}/hide")
    @Operation(summary = "Masque ou refuse la reponse: jamais projetee, ni transmise au vote/jury")
    public AnswerModerationResponse hide(@PathVariable UUID answerId) {
        return AnswerModerationResponse.from(answerModerationService.hide(answerId));
    }

    @PutMapping("/api/v1/staff/answers/{answerId}/content")
    @Operation(summary = "Corrige le contenu (faute de frappe, texte illisible) sans changer le sens")
    public AnswerModerationResponse correct(
            @PathVariable UUID answerId, @Valid @RequestBody AnswerCorrectionRequest request) {
        return AnswerModerationResponse.from(answerModerationService.correct(answerId, request.content()));
    }

    @PostMapping("/api/v1/staff/questions/{questionId}/teams/{teamId}/relaunch")
    @Operation(summary = "Relance une equipe: reponse remise a vide, plus personne n'a la main")
    public AnswerModerationResponse relaunchTeam(@PathVariable UUID questionId, @PathVariable UUID teamId) {
        return AnswerModerationResponse.from(answerModerationService.relaunchTeam(questionId, teamId));
    }
}
