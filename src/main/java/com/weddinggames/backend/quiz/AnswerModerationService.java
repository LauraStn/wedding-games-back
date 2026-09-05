package com.weddinggames.backend.quiz;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intervenant-facing moderation of team answers before they ever reach projection, vote or jury:
 * accept, hide ("masquer" a borderline answer or "refuser" an inappropriate one - same effect,
 * see {@link com.weddinggames.backend.game.AnswerModerationStatus}), correct a typo without
 * changing its meaning, or relaunch a team for a fresh attempt. Closing the input itself is the
 * existing question-close action (see QuestionStaffController) - moderation doesn't duplicate it.
 */
@Service
public class AnswerModerationService {

    private final AnswerRepository answerRepository;

    public AnswerModerationService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public List<Answer> listForQuestion(UUID questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    @Transactional
    public Answer accept(UUID answerId) {
        Answer answer = get(answerId);
        answer.accept();
        return answer;
    }

    @Transactional
    public Answer hide(UUID answerId) {
        Answer answer = get(answerId);
        answer.hide();
        return answer;
    }

    @Transactional
    public Answer correct(UUID answerId, String content) {
        Answer answer = get(answerId);
        answer.setContent(content);
        return answer;
    }

    @Transactional
    public Answer relaunchTeam(UUID questionId, UUID teamId) {
        Answer answer = answerRepository
                .findByQuestionIdAndTeamId(questionId, teamId)
                .orElseThrow(() -> new NotFoundException("Aucune reponse a relancer pour cette equipe."));
        answer.relaunch();
        return answer;
    }

    private Answer get(UUID answerId) {
        return answerRepository.findById(answerId).orElseThrow(() -> new NotFoundException("Reponse introuvable."));
    }
}
