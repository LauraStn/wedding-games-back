CREATE TABLE score (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    game_id    UUID REFERENCES game (id) ON DELETE CASCADE,
    team_id    UUID NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    points     INTEGER NOT NULL,
    reason     VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_score_event_id ON score (event_id);
CREATE INDEX ix_score_team_id ON score (team_id);
