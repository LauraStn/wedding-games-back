CREATE TABLE team (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    character_id UUID REFERENCES game_character (id) ON DELETE SET NULL,
    label        VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_team_event_id ON team (event_id);
CREATE INDEX ix_team_character_id ON team (character_id);
