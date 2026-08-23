CREATE TABLE answer (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id   UUID NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    team_id       UUID NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    content       VARCHAR(1000) NOT NULL,
    submitted_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_answer_question_team UNIQUE (question_id, team_id)
);

CREATE INDEX ix_answer_question_id ON answer (question_id);
