package com.weddinggames.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.game.dto.GameCreateRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for game creation and transition delegation. */
class GameServiceTest {

    private GameRepository gameRepository;
    private WeddingEventRepository weddingEventRepository;
    private GameService service;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        weddingEventRepository = mock(WeddingEventRepository.class);
        service = new GameService(gameRepository, weddingEventRepository);
    }

    @Test
    void createsAGameForAnExistingEvent() {
        UUID eventId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(gameRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Game created = service.create(eventId, new GameCreateRequest(GameType.QUIZ, "Quiz absurde", 0));

        assertThat(created.getTitle()).isEqualTo("Quiz absurde");
        assertThat(created.getType()).isEqualTo(GameType.QUIZ);
    }

    @Test
    void rejectsCreationForAnUnknownEvent() {
        UUID eventId = UUID.randomUUID();
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(eventId, new GameCreateRequest(GameType.QUIZ, "Quiz", 0)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void startDelegatesToTheEntitysGuardedTransition() {
        UUID id = UUID.randomUUID();
        Game game = new Game(mock(WeddingEvent.class), GameType.QUIZ, "Quiz", 0);
        when(gameRepository.findById(id)).thenReturn(Optional.of(game));

        Game result = service.start(id);

        assertThat(result.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.getPhase()).isEqualTo(GamePhase.PREPARATION);
    }

    @Test
    void nextQuestionDelegatesToTheEntitysGuardedTransition() {
        UUID id = UUID.randomUUID();
        Game game = new Game(mock(WeddingEvent.class), GameType.QUIZ, "Quiz", 0);
        game.start();
        when(gameRepository.findById(id)).thenReturn(Optional.of(game));

        Game result = service.nextQuestion(id);

        assertThat(result.getPhase()).isEqualTo(GamePhase.QUESTION);
    }
}
