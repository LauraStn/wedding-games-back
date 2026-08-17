package com.weddinggames.backend.staff.dto;

import com.weddinggames.backend.staff.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Password is optional: blank/null leaves the current password unchanged. */
public record StaffAccountUpdateRequest(
        @NotBlank @Size(max = 150) String displayName,
        @NotNull StaffRole role,
        boolean active,
        @Size(min = 8, max = 200) String password) {}
