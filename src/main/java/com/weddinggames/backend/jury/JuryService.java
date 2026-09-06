package com.weddinggames.backend.jury;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.jury.dto.JuryPointsRequest;
import com.weddinggames.backend.score.ScoreService;
import com.weddinggames.backend.score.dto.ScoreAwardRequest;
import com.weddinggames.backend.vote.FinalistService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jury workflow for a question: pick a winning answer among the finalists, confirm it (awarding
 * the round's points), optionally award a further bonus, and reveal the winning team. Scoring
 * itself is not duplicated here - both confirm and bonus delegate to the existing generic {@code
 * ScoreService} (ASST-165), which is exactly what it was built to be reused for.
 */
@Service
public class JuryService {

    private final JuryDecisionRepository decisionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FinalistService finalistService;
    private final ScoreService scoreService;

    public JuryService(
            JuryDecisionRepository decisionRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            FinalistService finalistService,
            ScoreService scoreService) {
        this.decisionRepository = decisionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.finalistService = finalistService;
        this.scoreService = scoreService;
    }

    @Transactional
    public JuryDecision getOrCreate(UUID questionId) {
        return decisionRepository.findByQuestionId(questionId).orElseGet(() -> {
            Question question = questionRepository
                    .findById(questionId)
                    .orElseThrow(() -> new NotFoundException("Question introuvable."));
            return decisionRepository.save(new JuryDecision(question));
        });
    }

    @Transactional
    public JuryDecision choose(UUID questionId, UUID answerId) {
        JuryDecision decision = getOrCreate(questionId);
        Answer answer =
                answerRepository.findById(answerId).orElseThrow(() -> new NotFoundException("Reponse introuvable."));

        boolean isFinalist = finalistService.computeFinalists(questionId).stream()
                .anyMatch(finalist -> finalist.answer().getId().equals(answerId));
        if (!isFinalist) {
            throw new BusinessRuleViolationException(
                    "ANSWER_NOT_A_FINALIST", "Cette reponse ne fait pas partie des finalistes de cette question.");
        }

        decision.choose(answer);
        return decision;
    }

    @Transactional
    public JuryDecision confirm(UUID questionId, JuryPointsRequest request) {
        JuryDecision decision = getOrCreate(questionId);
        decision.confirm();
        awardPoints(decision, request, "Reponse gagnante");
        return decision;
    }

    @Transactional
    public JuryDecision bonus(UUID questionId, JuryPointsRequest request) {
        JuryDecision decision = getOrCreate(questionId);
        if (decision.getStatus() != JuryDecisionStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "JURY_DECISION_NOT_CONFIRMED", "Il faut confirmer la decision avant d'attribuer un bonus.");
        }
        awardPoints(decision, request, "Bonus jury");
        return decision;
    }

    @Transactional
    public JuryDecision reveal(UUID questionId) {
        JuryDecision decision = getOrCreate(questionId);
        decision.reveal();
        return decision;
    }

    private void awardPoints(JuryDecision decision, JuryPointsRequest request, String defaultReason) {
        Answer chosenAnswer = decision.getChosenAnswer();
        Question question = decision.getQuestion();
        String reason = request.reason() != null ? request.reason() : defaultReason;
        scoreService.award(
                question.getGame().getEvent().getId(),
                new ScoreAwardRequest(
                        question.getGame().getId(), chosenAnswer.getTeam().getId(), request.points(), reason));
    }
}
