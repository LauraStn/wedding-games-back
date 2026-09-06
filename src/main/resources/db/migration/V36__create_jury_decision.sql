CREATE TABLE jury_decision (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id       UUID NOT NULL UNIQUE REFERENCES question (id) ON DELETE CASCADE,
    chosen_answer_id  UUID REFERENCES answer (id) ON DELETE SET NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    revealed          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_jury_decision_status CHECK (status IN ('PENDING', 'CHOSEN', 'CONFIRMED'))
);

CREATE INDEX ix_jury_decision_question_id ON jury_decision (question_id);
