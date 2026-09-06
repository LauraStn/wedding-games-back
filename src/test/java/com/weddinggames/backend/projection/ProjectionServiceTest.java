package com.weddinggames.backend.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.blindtest.BlindTestVariant;
import com.weddinggames.backend.blindtest.Track;
import com.weddinggames.backend.blindtest.TrackRepository;
import com.weddinggames.backend.blindtest.TrackStaffService;
import com.weddinggames.backend.blindtest.TrackStatus;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import com.weddinggames.backend.game.GameStatus;
import com.weddinggames.backend.game.GameType;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.game.QuestionStatus;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.lobby.LobbyStatus;
import com.weddinggames.backend.projection.dto.ProjectionResponse;
import com.weddinggames.backend.score.ScoreService;
import com.weddinggames.backend.score.dto.PodiumEntryResponse;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.vote.FinalistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the read-only projection aggregate. */
class ProjectionServiceTest {

    private LobbyRepository lobbyRepository;
    private GameRepository gameRepository;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private TrackRepository trackRepository;
    private TrackStaffService trackStaffService;
    private FinalistService finalistService;
    private ScoreService scoreService;
    private ProjectionService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        lobbyRepository = mock(LobbyRepository.class);
        gameRepository = mock(GameRepository.class);
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        trackRepository = mock(TrackRepository.class);
        trackStaffService = mock(TrackStaffService.class);
        finalistService = mock(FinalistService.class);
        scoreService = mock(ScoreService.class);
        service = new ProjectionService(
                lobbyRepository,
                gameRepository,
                questionRepository,
                answerRepository,
                trackRepository,
                trackStaffService,
                finalistService,
                scoreService);

        eventId = UUID.randomUUID();
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of());
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(scoreService.podium(eventId)).thenReturn(List.of());
    }

    private Game mockGame(GameStatus status, GameType type) {
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(UUID.randomUUID());
        when(game.getEvent()).thenReturn(event);
        when(game.getStatus()).thenReturn(status);
        when(game.getType()).thenReturn(type);
        when(game.getTitle()).thenReturn("Un jeu");
        when(game.getSequence()).thenReturn(0);
        return game;
    }

    @Test
    void reportsNoActiveGameWhenNoneIsInPlay() {
        ProjectionResponse response = service.get(eventId);

        assertThat(response.activeGame()).isNull();
        assertThat(response.activeTrack()).isNull();
        assertThat(response.anonymizedAnswers()).isEmpty();
        assertThat(response.finalists()).isEmpty();
    }

    @Test
    void reportsTheLobbyWhenOneExists() {
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        Lobby lobby = new Lobby(event);
        lobby.open(Instant.now());
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));

        ProjectionResponse response = service.get(eventId);

        assertThat(response.lobby().status()).isEqualTo(LobbyStatus.OPEN);
    }

    @Test
    void reportsTheActiveQuizGameWithAnonymizedAnswersAndFinalists() {
        Game game = mockGame(GameStatus.ACTIVE, GameType.QUIZ);
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of(game));

        Question pending = mock(Question.class);
        when(pending.getStatus()).thenReturn(QuestionStatus.PENDING);
        Question active = mock(Question.class);
        when(active.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        UUID questionId = UUID.randomUUID();
        when(active.getId()).thenReturn(questionId);
        when(questionRepository.findByGameIdOrderBySequence(game.getId())).thenReturn(List.of(pending, active));

        Answer answer = mock(Answer.class);
        when(answer.getModerationStatus()).thenReturn(com.weddinggames.backend.game.AnswerModerationStatus.ACCEPTED);
        when(answer.getId()).thenReturn(UUID.randomUUID());
        when(answer.getContent()).thenReturn("Une reponse absurde");
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(answer));

        Team team = mock(Team.class);
        when(team.getId()).thenReturn(UUID.randomUUID());
        when(answer.getTeam()).thenReturn(team);
        when(finalistService.computeFinalists(questionId))
                .thenReturn(List.of(new FinalistService.Finalist(answer, 3L)));

        ProjectionResponse response = service.get(eventId);

        assertThat(response.activeGame().type()).isEqualTo(GameType.QUIZ);
        assertThat(response.anonymizedAnswers()).hasSize(1);
        assertThat(response.anonymizedAnswers().get(0).content()).isEqualTo("Une reponse absurde");
        assertThat(response.finalists()).hasSize(1);
        assertThat(response.finalists().get(0).voteCount()).isNull();
        assertThat(response.activeTrack()).isNull();
    }

    @Test
    void reportsTheActiveBlindTestTrackAndCountdown() {
        Game game = mockGame(GameStatus.ACTIVE, GameType.BLIND_TEST);
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of(game));
        when(questionRepository.findByGameIdOrderBySequence(game.getId())).thenReturn(List.of());

        Track track = new Track(game, "Freed from Desire", "Gala", BlindTestVariant.REVERSED, 0);
        track.activate();
        when(trackRepository.findFirstByGameIdAndStatusOrderBySequence(game.getId(), TrackStatus.ACTIVE))
                .thenReturn(Optional.of(track));
        when(trackStaffService.remainingSeconds(track)).thenReturn(15);

        ProjectionResponse response = service.get(eventId);

        assertThat(response.activeTrack()).isNotNull();
        assertThat(response.activeTrack().title()).isEqualTo("Freed from Desire");
        assertThat(response.activeTrack().remainingSeconds()).isEqualTo(15);
    }

    @Test
    void alwaysIncludesThePodium() {
        when(scoreService.podium(eventId)).thenReturn(List.of(new PodiumEntryResponse(UUID.randomUUID(), "Equipe", 10L, 1)));

        ProjectionResponse response = service.get(eventId);

        assertThat(response.podium()).hasSize(1);
    }
}
