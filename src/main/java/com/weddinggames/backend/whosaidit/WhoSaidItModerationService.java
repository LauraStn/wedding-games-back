package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intervenant-facing moderation of guest-proposed "Who Said It" questions: accept, reject
 * (either can be reconsidered into the other), or correct a typo without changing the meaning.
 * Marking a question PLAYED is not exposed here: it happens as a side effect of the random
 * selection for play (a separate, downstream ticket), not as a standalone moderation action.
 */
@Service
public class WhoSaidItModerationService {

    private final WhoSaidItQuestionRepository questionRepository;

    public WhoSaidItModerationService(WhoSaidItQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<WhoSaidItQuestion> listForEvent(UUID eventId) {
        return questionRepository.findByEventId(eventId);
    }

    @Transactional
    public WhoSaidItQuestion accept(UUID questionId) {
        WhoSaidItQuestion question = get(questionId);
        question.accept();
        return question;
    }

    @Transactional
    public WhoSaidItQuestion reject(UUID questionId) {
        WhoSaidItQuestion question = get(questionId);
        question.reject();
        return question;
    }

    @Transactional
    public WhoSaidItQuestion correct(UUID questionId, String content) {
        WhoSaidItQuestion question = get(questionId);
        question.correct(content);
        return question;
    }

    private WhoSaidItQuestion get(UUID questionId) {
        return questionRepository
                .findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question introuvable."));
    }
}
