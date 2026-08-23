CREATE TABLE team_member (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id        UUID NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_member_participant UNIQUE (participant_id)
);

CREATE INDEX ix_team_member_team_id ON team_member (team_id);
