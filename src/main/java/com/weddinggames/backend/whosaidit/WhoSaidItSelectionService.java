package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws a random ACCEPTED "Who Said It" question for the intervenant to play live, and marks it
 * PLAYED so it is never drawn again. Deliberately does not materialize a {@code game.Question}
 * (which already carries {@code QuestionSource.GUEST}/{@code authorParticipant}/{@code
 * revealAuthor} from the game engine's original schema) - nothing in this ticket asks for wiring
 * into a specific {@code Game}, and doing so would require assumptions this ticket doesn't make.
 */
@Service
public class WhoSaidItSelectionService {

    private final WhoSaidItQuestionRepository questionRepository;
    private final Random random;

    public WhoSaidItSelectionService(WhoSaidItQuestionRepository questionRepository, Random random) {
        this.questionRepository = questionRepository;
        this.random = random;
    }

    @Transactional
    public WhoSaidItQuestion selectRandom(UUID eventId) {
        List<WhoSaidItQuestion> accepted =
                questionRepository.findByEventIdAndStatus(eventId, WhoSaidItQuestionStatus.ACCEPTED);
        if (accepted.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_ACCEPTED_WHO_SAID_IT_QUESTION", "Aucune question acceptee disponible pour le tirage.");
        }
        WhoSaidItQuestion selected = accepted.get(random.nextInt(accepted.size()));
        selected.markPlayed();
        return selected;
    }
}
