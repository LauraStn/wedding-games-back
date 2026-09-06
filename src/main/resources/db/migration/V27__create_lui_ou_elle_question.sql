CREATE TABLE lui_ou_elle_question (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_lui_ou_elle_question_event_id ON lui_ou_elle_question (event_id);
CREATE INDEX ix_lui_ou_elle_question_author_id ON lui_ou_elle_question (author_id);
