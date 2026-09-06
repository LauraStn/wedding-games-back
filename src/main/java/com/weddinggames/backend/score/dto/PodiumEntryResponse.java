package com.weddinggames.backend.score.dto;

import java.util.UUID;

public record PodiumEntryResponse(UUID teamId, String teamLabel, long totalPoints, int rank) {}
