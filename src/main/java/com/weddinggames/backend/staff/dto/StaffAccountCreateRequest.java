package com.weddinggames.backend.staff.dto;

import com.weddinggames.backend.staff.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffAccountCreateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 200) String password,
        @NotBlank @Size(max = 150) String displayName,
        @NotNull StaffRole role) {}
