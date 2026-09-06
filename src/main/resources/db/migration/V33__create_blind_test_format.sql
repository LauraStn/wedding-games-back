CREATE TABLE blind_test_format (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id                  UUID NOT NULL UNIQUE REFERENCES game (id) ON DELETE CASCADE,
    round_duration_seconds   INTEGER NOT NULL DEFAULT 30,
    points_per_correct_guess INTEGER NOT NULL DEFAULT 10,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
