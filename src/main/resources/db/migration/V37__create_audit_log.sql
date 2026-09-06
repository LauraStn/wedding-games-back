-- No foreign keys on purpose: an audit trail must survive the deletion of whatever it references
-- (the staff account, the event, the affected entity), so staff_account_id/event_id/entity_id are
-- plain UUID columns, and staff_display_name snapshots the actor's name at the time of the action.
CREATE TABLE audit_log (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_account_id   UUID NOT NULL,
    staff_display_name VARCHAR(150) NOT NULL,
    action             VARCHAR(50) NOT NULL,
    event_id           UUID,
    entity_id          UUID,
    details            VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_event_id ON audit_log (event_id);
CREATE INDEX ix_audit_log_created_at ON audit_log (created_at);
