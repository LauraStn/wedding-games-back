package com.weddinggames.backend.common.audit.dto;

import com.weddinggames.backend.common.audit.AuditAction;
import com.weddinggames.backend.common.audit.AuditLogEntry;
import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        UUID staffAccountId,
        String staffDisplayName,
        AuditAction action,
        UUID eventId,
        UUID entityId,
        String details,
        Instant createdAt) {

    public static AuditLogEntryResponse from(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.getId(),
                entry.getStaffAccountId(),
                entry.getStaffDisplayName(),
                entry.getAction(),
                entry.getEventId(),
                entry.getEntityId(),
                entry.getDetails(),
                entry.getCreatedAt());
    }
}
