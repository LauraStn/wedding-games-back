CREATE TABLE lobby (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL,
    opened_at  TIMESTAMPTZ,
    closed_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lobby_event_id UNIQUE (event_id),
    CONSTRAINT ck_lobby_status CHECK (status IN ('CLOSED', 'OPEN', 'LOCKED'))
);
