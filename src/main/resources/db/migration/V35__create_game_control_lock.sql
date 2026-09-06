CREATE TABLE game_control_lock (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id        UUID NOT NULL UNIQUE REFERENCES game (id) ON DELETE CASCADE,
    holder_id      UUID REFERENCES staff_account (id) ON DELETE SET NULL,
    claimed_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
