package com.weddinggames.backend.projection.dto;

import com.weddinggames.backend.blindtest.dto.TrackStateResponse;
import com.weddinggames.backend.game.dto.GameResponse;
import com.weddinggames.backend.lobby.dto.LobbyResponse;
import com.weddinggames.backend.score.dto.PodiumEntryResponse;
import com.weddinggames.backend.vote.dto.FinalistResponse;
import com.weddinggames.backend.vote.dto.VotingOptionResponse;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregate for the projection screen: lobby state, the active game, its running
 * countdown (blind test only), the current question's anonymized answers and top-3 finalists
 * (quiz only), and the event's podium. Any field not applicable to what's currently happening is
 * simply {@code null}/empty - never an error.
 */
public record ProjectionResponse(
        UUID eventId,
        LobbyResponse lobby,
        GameResponse activeGame,
        TrackStateResponse activeTrack,
        List<VotingOptionResponse> anonymizedAnswers,
        List<FinalistResponse> finalists,
        List<PodiumEntryResponse> podium) {}
