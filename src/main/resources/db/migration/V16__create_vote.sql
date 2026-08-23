CREATE TABLE vote (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id           UUID NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    answer_id             UUID NOT NULL REFERENCES answer (id) ON DELETE CASCADE,
    voter_participant_id  UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vote_question_voter UNIQUE (question_id, voter_participant_id)
);

CREATE INDEX ix_vote_question_id ON vote (question_id);
CREATE INDEX ix_vote_answer_id ON vote (answer_id);
