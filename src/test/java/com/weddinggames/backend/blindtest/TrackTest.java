package com.weddinggames.backend.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Game;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure unit test for the PENDING/ACTIVE/CLOSED round lifecycle and timer guard. */
class TrackTest {

    private Track newTrack() {
        return new Track(mock(Game.class), "Freed from Desire", "Gala", BlindTestVariant.REVERSED, 0);
    }

    @Test
    void startsAsPending() {
        assertThat(newTrack().getStatus()).isEqualTo(TrackStatus.PENDING);
    }

    @Test
    void activatesFromPending() {
        Track track = newTrack();
        track.activate();
        assertThat(track.getStatus()).isEqualTo(TrackStatus.ACTIVE);
    }

    @Test
    void cannotActivateTwice() {
        Track track = newTrack();
        track.activate();
        assertThatThrownBy(track::activate).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void closesFromActive() {
        Track track = newTrack();
        track.activate();
        track.close();
        assertThat(track.getStatus()).isEqualTo(TrackStatus.CLOSED);
    }

    @Test
    void cannotCloseAPendingTrack() {
        assertThatThrownBy(newTrack()::close).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotStartTheTimerBeforeActivating() {
        assertThatThrownBy(() -> newTrack().startTimer(Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void startsTheTimerOnceActive() {
        Track track = newTrack();
        track.activate();
        Instant now = Instant.now();

        track.startTimer(now);

        assertThat(track.getTimerStartedAt()).isEqualTo(now);
    }

    @Test
    void cannotStartTheTimerOnceClosed() {
        Track track = newTrack();
        track.activate();
        track.close();

        assertThatThrownBy(() -> track.startTimer(Instant.now())).isInstanceOf(BusinessRuleViolationException.class);
    }
}
