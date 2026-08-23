CREATE TABLE question (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id                UUID NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    prompt                 VARCHAR(1000) NOT NULL,
    sequence               INTEGER NOT NULL DEFAULT 0,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source                 VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    author_participant_id  UUID REFERENCES participant (id) ON DELETE SET NULL,
    reveal_author          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_question_status CHECK (status IN ('PENDING', 'ACTIVE', 'CLOSED')),
    CONSTRAINT ck_question_source CHECK (source IN ('ADMIN', 'GUEST'))
);

CREATE INDEX ix_question_game_id ON question (game_id);
