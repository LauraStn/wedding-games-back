package com.weddinggames.backend.blindtest.dto;

import com.weddinggames.backend.blindtest.BlindTestVariant;
import com.weddinggames.backend.blindtest.Track;
import java.util.UUID;

public record TrackResponse(
        UUID id, UUID gameId, String title, String artist, BlindTestVariant variant, int sequence) {

    public static TrackResponse from(Track track) {
        return new TrackResponse(
                track.getId(),
                track.getGame().getId(),
                track.getTitle(),
                track.getArtist(),
                track.getVariant(),
                track.getSequence());
    }
}
