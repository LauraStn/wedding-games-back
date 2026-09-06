package com.weddinggames.backend.control.dto;

import com.weddinggames.backend.control.GameControlLock;
import java.time.Instant;
import java.util.UUID;

public record GameControlLockResponse(
        UUID gameId, UUID holderStaffAccountId, String holderDisplayName, Instant claimedAt) {

    public static GameControlLockResponse from(GameControlLock lock) {
        var holder = lock.getHolder();
        return new GameControlLockResponse(
                lock.getGame().getId(),
                holder != null ? holder.getId() : null,
                holder != null ? holder.getDisplayName() : null,
                lock.getClaimedAt());
    }
}
