package com.weddinggames.backend.jury.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JuryChooseRequest(@NotNull UUID answerId) {}
