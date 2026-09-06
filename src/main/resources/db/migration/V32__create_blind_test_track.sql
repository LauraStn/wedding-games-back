CREATE TABLE track (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id    UUID NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    artist     VARCHAR(200) NOT NULL,
    variant    VARCHAR(30) NOT NULL,
    sequence   INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_track_variant CHECK (variant IN ('SLOWED_DOWN', 'REVERSED', 'LYRICS_CONTINUATION'))
);

CREATE INDEX ix_track_game_id ON track (game_id);
