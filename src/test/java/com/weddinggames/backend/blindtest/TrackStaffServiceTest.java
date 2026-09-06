package com.weddinggames.backend.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for round activation and the countdown timer. */
class TrackStaffServiceTest {

    private TrackRepository trackRepository;
    private BlindTestFormatRepository formatRepository;
    private GameRepository gameRepository;
    private Clock clock;
    private TrackStaffService service;
    private UUID gameId;
    private Game game;
    private Instant now;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class);
        formatRepository = mock(BlindTestFormatRepository.class);
        gameRepository = mock(GameRepository.class);
        now = Instant.parse("2026-01-01T20:00:00Z");
        clock = Clock.fixed(now, ZoneOffset.UTC);
        BlindTestFormatService formatService = new BlindTestFormatService(formatRepository, gameRepository);
        service = new TrackStaffService(trackRepository, formatService, clock);

        gameId = UUID.randomUUID();
        game = mock(Game.class);
        when(game.getId()).thenReturn(gameId);
    }

    private Track existingTrack(UUID trackId) {
        Track track = new Track(game, "Freed from Desire", "Gala", BlindTestVariant.REVERSED, 0);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
        return track;
    }

    @Test
    void activatesAPendingTrack() {
        UUID trackId = UUID.randomUUID();
        existingTrack(trackId);

        Track activated = service.activate(trackId);

        assertThat(activated.getStatus()).isEqualTo(TrackStatus.ACTIVE);
    }

    @Test
    void rejectsActionsOnAnUnknownTrack() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(trackId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCurrentActiveReturnsTheActiveTrackForAGame() {
        Track active = existingTrack(UUID.randomUUID());
        active.activate();
        when(trackRepository.findFirstByGameIdAndStatusOrderBySequence(gameId, TrackStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        Optional<Track> result = service.getCurrentActive(gameId);

        assertThat(result).contains(active);
    }

    @Test
    void remainingSecondsIsNullWhenTheTimerHasNotStarted() {
        Track track = existingTrack(UUID.randomUUID());
        track.activate();

        assertThat(service.remainingSeconds(track)).isNull();
    }

    @Test
    void remainingSecondsCountsDownFromTheConfiguredRoundDuration() {
        BlindTestFormat format = new BlindTestFormat(game);
        format.setRoundDurationSeconds(30);
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.of(format));
        Track track = existingTrack(UUID.randomUUID());
        track.activate();
        track.startTimer(now.minusSeconds(10));

        assertThat(service.remainingSeconds(track)).isEqualTo(20);
    }

    @Test
    void remainingSecondsNeverGoesNegative() {
        BlindTestFormat format = new BlindTestFormat(game);
        format.setRoundDurationSeconds(30);
        when(formatRepository.findByGameId(gameId)).thenReturn(Optional.of(format));
        Track track = existingTrack(UUID.randomUUID());
        track.activate();
        track.startTimer(now.minusSeconds(45));

        assertThat(service.remainingSeconds(track)).isZero();
    }
}
