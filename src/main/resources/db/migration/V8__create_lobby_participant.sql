CREATE TABLE lobby_participant (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lobby_id           UUID NOT NULL REFERENCES lobby (id) ON DELETE CASCADE,
    participant_id     UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    arrived_at         TIMESTAMPTZ NOT NULL,
    last_activity_at   TIMESTAMPTZ NOT NULL,
    connection_status  VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lobby_participant UNIQUE (lobby_id, participant_id),
    CONSTRAINT ck_lobby_participant_connection_status CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'LATE'))
);

CREATE INDEX ix_lobby_participant_lobby_id ON lobby_participant (lobby_id);
