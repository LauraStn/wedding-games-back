package com.weddinggames.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.game.CurrentGameService.CurrentGameState;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the guest-facing "what is the room playing" lookup. */
class CurrentGameServiceTest {

    private ParticipantRepository participantRepository;
    private GameRepository gameRepository;
    private QuestionRepository questionRepository;
    private CurrentGameService service;

    private final UUID participantId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        gameRepository = mock(GameRepository.class);
        questionRepository = mock(QuestionRepository.class);
        service = new CurrentGameService(participantRepository, gameRepository, questionRepository);

        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        Participant participant = mock(Participant.class);
        when(participant.getEvent()).thenReturn(event);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
    }

    private Game mockGame(GameStatus status) {
        Game game = mock(Game.class);
        when(game.getStatus()).thenReturn(status);
        when(game.getId()).thenReturn(UUID.randomUUID());
        return game;
    }

    private Question mockQuestion(QuestionStatus status) {
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(status);
        return question;
    }

    @Test
    void throwsWhenTheParticipantIsUnknown() {
        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentGameFor(participantId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void returnsNothingWhenTheEventHasNoGame() {
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of());

        CurrentGameState state = service.currentGameFor(participantId);

        assertThat(state.game()).isNull();
        assertThat(state.question()).isNull();
    }

    @Test
    void ignoresGamesThatAreNotActiveOrPaused() {
        Game draft = mockGame(GameStatus.DRAFT);
        Game finished = mockGame(GameStatus.FINISHED);
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of(draft, finished));

        CurrentGameState state = service.currentGameFor(participantId);

        assertThat(state.game()).isNull();
        assertThat(state.question()).isNull();
    }

    @Test
    void picksTheFirstInPlayGameAndReportsNoQuestionWhileEveryQuestionIsPending() {
        Game draft = mockGame(GameStatus.DRAFT);
        Game active = mockGame(GameStatus.ACTIVE);
        Question pendingOne = mockQuestion(QuestionStatus.PENDING);
        Question pendingTwo = mockQuestion(QuestionStatus.PENDING);
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of(draft, active));
        when(questionRepository.findByGameIdOrderBySequence(active.getId()))
                .thenReturn(List.of(pendingOne, pendingTwo));

        CurrentGameState state = service.currentGameFor(participantId);

        assertThat(state.game()).isSameAs(active);
        assertThat(state.question()).isNull();
    }

    @Test
    void currentQuestionIsTheHighestSequenceQuestionThatHasLeftPending() {
        Game active = mockGame(GameStatus.PAUSED);
        Question closed = mockQuestion(QuestionStatus.CLOSED);
        Question live = mockQuestion(QuestionStatus.ACTIVE);
        Question notStarted = mockQuestion(QuestionStatus.PENDING);
        when(gameRepository.findByEventIdOrderBySequence(eventId)).thenReturn(List.of(active));
        when(questionRepository.findByGameIdOrderBySequence(active.getId()))
                .thenReturn(List.of(closed, live, notStarted));

        CurrentGameState state = service.currentGameFor(participantId);

        assertThat(state.game()).isSameAs(active);
        assertThat(state.question()).isSameAs(live);
    }
}
