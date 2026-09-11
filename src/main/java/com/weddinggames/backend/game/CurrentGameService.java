package com.weddinggames.backend.game;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers the one question a guest's screen needs between phases: "what is the room playing right
 * now, and on which question?". Every participant-facing gameplay endpoint ({@code /quiz/...},
 * {@code /vote/...}) is addressed by {@code questionId}, but nothing else exposes that id to a
 * {@code PARTICIPANT} - this does.
 *
 * <p>Read-only and forgiving: no active game (or no started question yet) is {@code null}, never an
 * error. The active-game / current-question selection mirrors
 * {@link com.weddinggames.backend.projection.ProjectionService} so guests and the projection screen
 * always agree on what is live.
 */
@Service
public class CurrentGameService {

    private static final Set<GameStatus> IN_PLAY = Set.of(GameStatus.ACTIVE, GameStatus.PAUSED);

    private final ParticipantRepository participantRepository;
    private final GameRepository gameRepository;
    private final QuestionRepository questionRepository;

    public CurrentGameService(
            ParticipantRepository participantRepository,
            GameRepository gameRepository,
            QuestionRepository questionRepository) {
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
        this.questionRepository = questionRepository;
    }

    /** The live game and current question for the event the given participant belongs to. */
    public record CurrentGameState(Game game, Question question) {}

    @Transactional(readOnly = true)
    public CurrentGameState currentGameFor(UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));

        Game game = gameRepository.findByEventIdOrderBySequence(participant.getEvent().getId()).stream()
                .filter(candidate -> IN_PLAY.contains(candidate.getStatus()))
                .findFirst()
                .orElse(null);

        Question question = game == null ? null : currentQuestion(game.getId());
        return new CurrentGameState(game, question);
    }

    /** The most recently reached (highest-sequence) question that has left {@link QuestionStatus#PENDING}. */
    private Question currentQuestion(UUID gameId) {
        Question current = null;
        for (Question question : questionRepository.findByGameIdOrderBySequence(gameId)) {
            if (question.getStatus() != QuestionStatus.PENDING) {
                current = question;
            }
        }
        return current;
    }
}
