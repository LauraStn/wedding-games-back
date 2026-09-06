package com.weddinggames.backend.jury.dto;

/** Points to award: used both for confirming the main choice and for an optional later bonus. */
public record JuryPointsRequest(int points, String reason) {}
