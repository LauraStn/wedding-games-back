package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intervenant-facing moderation of guest-proposed "Lui ou Elle" questions: accept, reject
 * (either can be reconsidered into the other), or correct a typo without changing the meaning.
 * Marking a question PLAYED is not exposed here: it happens as a side effect of the random
 * selection for play (a separate, downstream ticket), not as a standalone moderation action.
 */
@Service
public class LuiOuElleModerationService {

    private final LuiOuElleQuestionRepository questionRepository;

    public LuiOuElleModerationService(LuiOuElleQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<LuiOuElleQuestion> listForEvent(UUID eventId) {
        return questionRepository.findByEventId(eventId);
    }

    @Transactional
    public LuiOuElleQuestion accept(UUID questionId) {
        LuiOuElleQuestion question = get(questionId);
        question.accept();
        return question;
    }

    @Transactional
    public LuiOuElleQuestion reject(UUID questionId) {
        LuiOuElleQuestion question = get(questionId);
        question.reject();
        return question;
    }

    @Transactional
    public LuiOuElleQuestion correct(UUID questionId, String content) {
        LuiOuElleQuestion question = get(questionId);
        question.correct(content);
        return question;
    }

    private LuiOuElleQuestion get(UUID questionId) {
        return questionRepository
                .findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question introuvable."));
    }
}
