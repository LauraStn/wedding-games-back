package com.weddinggames.backend.blindtest.dto;

import com.weddinggames.backend.blindtest.BlindTestVariant;
import com.weddinggames.backend.blindtest.Track;
import com.weddinggames.backend.blindtest.TrackStatus;
import java.util.UUID;

/** A track's round state (status, remaining countdown), shown to staff and the projection screen. */
public record TrackStateResponse(
        UUID id,
        UUID gameId,
        String title,
        String artist,
        BlindTestVariant variant,
        int sequence,
        TrackStatus status,
        Integer remainingSeconds) {

    public static TrackStateResponse from(Track track, Integer remainingSeconds) {
        return new TrackStateResponse(
                track.getId(),
                track.getGame().getId(),
                track.getTitle(),
                track.getArtist(),
                track.getVariant(),
                track.getSequence(),
                track.getStatus(),
                remainingSeconds);
    }
}
