package com.weddinggames.backend.lobby.dto;

import com.weddinggames.backend.lobby.LobbyStatus;

/**
 * Lobby state as seen by a participant: no per-guest presence list here (that stays
 * staff-only), just the aggregate the waiting-room screen needs.
 */
public record LobbyParticipantStatusResponse(LobbyStatus status, long presentCount, String welcomeMessage) {}
