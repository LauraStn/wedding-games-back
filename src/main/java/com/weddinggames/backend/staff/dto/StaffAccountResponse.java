package com.weddinggames.backend.staff.dto;

import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffRole;
import java.time.Instant;
import java.util.UUID;

public record StaffAccountResponse(
        UUID id, String username, String displayName, StaffRole role, boolean active, Instant createdAt) {

    public static StaffAccountResponse from(StaffAccount account) {
        return new StaffAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.isActive(),
                account.getCreatedAt());
    }
}
