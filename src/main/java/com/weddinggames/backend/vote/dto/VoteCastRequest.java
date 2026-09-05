package com.weddinggames.backend.vote.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoteCastRequest(@NotNull UUID answerId) {}
