package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws a random ACCEPTED "Lui ou Elle" question for the intervenant to play live, and marks it
 * PLAYED so it is never drawn again. Deliberately does not materialize a {@code game.Question}
 * (which already carries {@code QuestionSource.GUEST}/{@code authorParticipant}/{@code
 * revealAuthor} from the game engine's original schema) - nothing in this ticket asks for wiring
 * into a specific {@code Game}, and doing so would require assumptions this ticket doesn't make.
 */
@Service
public class LuiOuElleSelectionService {

    private final LuiOuElleQuestionRepository questionRepository;
    private final Random random;

    public LuiOuElleSelectionService(LuiOuElleQuestionRepository questionRepository, Random random) {
        this.questionRepository = questionRepository;
        this.random = random;
    }

    @Transactional
    public LuiOuElleQuestion selectRandom(UUID eventId) {
        List<LuiOuElleQuestion> accepted =
                questionRepository.findByEventIdAndStatus(eventId, LuiOuElleQuestionStatus.ACCEPTED);
        if (accepted.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_ACCEPTED_LUI_OU_ELLE_QUESTION", "Aucune question acceptee disponible pour le tirage.");
        }
        LuiOuElleQuestion selected = accepted.get(random.nextInt(accepted.size()));
        selected.markPlayed();
        return selected;
    }
}
