package com.weddinggames.backend.projection;

import com.weddinggames.backend.blindtest.Track;
import com.weddinggames.backend.blindtest.TrackRepository;
import com.weddinggames.backend.blindtest.TrackStaffService;
import com.weddinggames.backend.blindtest.TrackStatus;
import com.weddinggames.backend.blindtest.dto.TrackStateResponse;
import com.weddinggames.backend.game.AnswerModerationStatus;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import com.weddinggames.backend.game.GameStatus;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.game.QuestionStatus;
import com.weddinggames.backend.game.dto.GameResponse;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.lobby.dto.LobbyResponse;
import com.weddinggames.backend.projection.dto.ProjectionResponse;
import com.weddinggames.backend.score.ScoreService;
import com.weddinggames.backend.vote.FinalistService;
import com.weddinggames.backend.vote.dto.FinalistResponse;
import com.weddinggames.backend.vote.dto.VotingOptionResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the read-only aggregate the projection screen polls. Purely a composition of existing
 * services/repositories - no new business rules, no mutation, so a {@code PROJECTION} account can
 * never affect state through it.
 */
@Service
public class ProjectionService {

    private static final Set<GameStatus> IN_PLAY = Set.of(GameStatus.ACTIVE, GameStatus.PAUSED);

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TrackRepository trackRepository;
    private final TrackStaffService trackStaffService;
    private final FinalistService finalistService;
    private final ScoreService scoreService;

    public ProjectionService(
            LobbyRepository lobbyRepository,
            GameRepository gameRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            TrackRepository trackRepository,
            TrackStaffService trackStaffService,
            FinalistService finalistService,
            ScoreService scoreService) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.trackRepository = trackRepository;
        this.trackStaffService = trackStaffService;
        this.finalistService = finalistService;
        this.scoreService = scoreService;
    }

    @Transactional(readOnly = true)
    public ProjectionResponse get(UUID eventId) {
        LobbyResponse lobby =
                lobbyRepository.findByEventId(eventId).map(LobbyResponse::from).orElse(null);
        Game activeGame = gameRepository.findByEventIdOrderBySequence(eventId).stream()
                .filter(game -> IN_PLAY.contains(game.getStatus()))
                .findFirst()
                .orElse(null);

        if (activeGame == null) {
            return new ProjectionResponse(eventId, lobby, null, null, List.of(), List.of(), scoreService.podium(eventId));
        }

        TrackStateResponse activeTrack = trackRepository
                .findFirstByGameIdAndStatusOrderBySequence(activeGame.getId(), TrackStatus.ACTIVE)
                .map(track -> TrackStateResponse.from(track, trackStaffService.remainingSeconds(track)))
                .orElse(null);

        Question currentQuestion = currentQuestion(activeGame);
        List<VotingOptionResponse> anonymizedAnswers = List.of();
        List<FinalistResponse> finalists = List.of();
        if (currentQuestion != null) {
            anonymizedAnswers = answerRepository.findByQuestionId(currentQuestion.getId()).stream()
                    .filter(answer -> answer.getModerationStatus() == AnswerModerationStatus.ACCEPTED)
                    .map(VotingOptionResponse::from)
                    .toList();
            finalists = finalistService.computeFinalists(currentQuestion.getId()).stream()
                    .map(finalist -> FinalistResponse.from(finalist, false))
                    .toList();
        }

        return new ProjectionResponse(
                eventId,
                lobby,
                GameResponse.from(activeGame),
                activeTrack,
                anonymizedAnswers,
                finalists,
                scoreService.podium(eventId));
    }

    /** The most recently touched (highest-sequence) non-pending question of the active game. */
    private Question currentQuestion(Game game) {
        List<Question> questions = questionRepository.findByGameIdOrderBySequence(game.getId());
        Question current = null;
        for (Question question : questions) {
            if (question.getStatus() != QuestionStatus.PENDING) {
                current = question;
            }
        }
        return current;
    }
}
