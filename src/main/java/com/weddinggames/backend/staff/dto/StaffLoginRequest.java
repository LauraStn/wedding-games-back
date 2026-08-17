package com.weddinggames.backend.staff.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(@NotBlank String username, @NotBlank String password) {}
