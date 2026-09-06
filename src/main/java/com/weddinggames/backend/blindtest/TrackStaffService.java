package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.common.exception.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intervenant-facing control of a blind test round: activating/closing a track (same PENDING ->
 * ACTIVE -> CLOSED shape as {@code Question}), and starting its countdown timer. Score entry
 * itself is not duplicated here: the intervenant awards points through the existing generic
 * {@code ScoreService} (see ASST-165), optionally referencing this game and noting the track in
 * the free-text reason - a blind test round doesn't need its own scoring mechanism.
 */
@Service
public class TrackStaffService {

    private final TrackRepository trackRepository;
    private final BlindTestFormatService formatService;
    private final Clock clock;

    public TrackStaffService(TrackRepository trackRepository, BlindTestFormatService formatService, Clock clock) {
        this.trackRepository = trackRepository;
        this.formatService = formatService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Track get(UUID trackId) {
        return trackRepository.findById(trackId).orElseThrow(() -> new NotFoundException("Morceau introuvable."));
    }

    @Transactional(readOnly = true)
    public Optional<Track> getCurrentActive(UUID gameId) {
        return trackRepository.findFirstByGameIdAndStatusOrderBySequence(gameId, TrackStatus.ACTIVE);
    }

    @Transactional
    public Track activate(UUID trackId) {
        Track track = get(trackId);
        track.activate();
        return track;
    }

    @Transactional
    public Track close(UUID trackId) {
        Track track = get(trackId);
        track.close();
        return track;
    }

    @Transactional
    public Track startTimer(UUID trackId) {
        Track track = get(trackId);
        track.startTimer(Instant.now(clock));
        return track;
    }

    /**
     * Seconds left in the track's countdown, clamped to zero once elapsed; {@code null} if the
     * timer hasn't been started yet.
     */
    @Transactional(readOnly = true)
    public Integer remainingSeconds(Track track) {
        if (track.getTimerStartedAt() == null) {
            return null;
        }
        BlindTestFormat format = formatService.getOrCreate(track.getGame().getId());
        long elapsed = Duration.between(track.getTimerStartedAt(), Instant.now(clock)).getSeconds();
        return (int) Math.max(0, format.getRoundDurationSeconds() - elapsed);
    }
}
