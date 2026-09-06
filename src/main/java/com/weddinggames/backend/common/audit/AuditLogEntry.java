package com.weddinggames.backend.common.audit;

import com.weddinggames.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * An append-only record of a sensitive administrative action. Deliberately not foreign-keyed to
 * the staff account/event/entity it references (see the migration): the trail must remain
 * readable even after the thing it describes is gone, so {@code staffDisplayName} snapshots the
 * actor's name at the time of the action rather than being looked up later.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry extends BaseEntity {

    @Column(name = "staff_account_id", nullable = false)
    private UUID staffAccountId;

    @Column(name = "staff_display_name", nullable = false, length = 150)
    private String staffDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(length = 500)
    private String details;

    protected AuditLogEntry() {}

    public AuditLogEntry(
            UUID staffAccountId,
            String staffDisplayName,
            AuditAction action,
            UUID eventId,
            UUID entityId,
            String details) {
        this.staffAccountId = staffAccountId;
        this.staffDisplayName = staffDisplayName;
        this.action = action;
        this.eventId = eventId;
        this.entityId = entityId;
        this.details = details;
    }

    public UUID getStaffAccountId() {
        return staffAccountId;
    }

    public String getStaffDisplayName() {
        return staffDisplayName;
    }

    public AuditAction getAction() {
        return action;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
