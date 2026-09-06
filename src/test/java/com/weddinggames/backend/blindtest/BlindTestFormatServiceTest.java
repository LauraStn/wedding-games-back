package com.weddinggames.backend.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.blindtest.dto.BlindTestFormatRequest;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the lazily-created round format config. */
class BlindTestFormatServiceTest {

    private BlindTestFormatRepository formatRepository;
    private GameRepository gameRepository;
    private BlindTestFormatService service;
    private UUID gameId;
    private Game game;

    @BeforeEach
    void setUp() {
        formatRepository = mock(BlindTestFormatRepository.class);
        gameRepository = mock(GameRepository.class);
        service = new BlindTestFormatService(formatRepository, gameRepository);
        gameId = UUID.randomUUID();
        game = mock(Game.class);
        when(formatRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsADefaultFormatOnFirstAccess() {
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.empty());
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        BlindTestFormat format = service.getOrCreate(gameId);

        assertThat(format.getRoundDurationSeconds()).isEqualTo(30);
        assertThat(format.getPointsPerCorrectGuess()).isEqualTo(10);
    }

    @Test
    void reusesTheExistingFormatOnSubsequentAccess() {
        BlindTestFormat existing = new BlindTestFormat(game);
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.of(existing));

        BlindTestFormat format = service.getOrCreate(gameId);

        assertThat(format).isSameAs(existing);
    }

    @Test
    void rejectsCreatingAFormatForAnUnknownGame() {
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.empty());
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreate(gameId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesTheRoundDurationAndPoints() {
        BlindTestFormat existing = new BlindTestFormat(game);
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.of(existing));

        BlindTestFormat updated = service.update(gameId, new BlindTestFormatRequest(45, 20));

        assertThat(updated.getRoundDurationSeconds()).isEqualTo(45);
        assertThat(updated.getPointsPerCorrectGuess()).isEqualTo(20);
    }
}
