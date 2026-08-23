CREATE TABLE game (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    type       VARCHAR(30) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    sequence   INTEGER NOT NULL DEFAULT 0,
    status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_game_type CHECK (type IN ('QUIZ', 'LUI_OU_ELLE', 'BLIND_TEST', 'CUSTOM')),
    CONSTRAINT ck_game_status CHECK (status IN ('DRAFT', 'READY', 'ACTIVE', 'PAUSED', 'FINISHED'))
);

CREATE INDEX ix_game_event_id ON game (event_id);
