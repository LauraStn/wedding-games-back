CREATE TABLE pairing_exclusion (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id         UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    participant_a_id UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    participant_b_id UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    reason           VARCHAR(300),
    exclusion_type   VARCHAR(20) NOT NULL,
    locked           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_pairing_exclusion_type CHECK (exclusion_type IN ('HARD', 'PREFERENCE')),
    CONSTRAINT ck_pairing_exclusion_distinct_participants CHECK (participant_a_id <> participant_b_id),
    CONSTRAINT uq_pairing_exclusion_pair UNIQUE (event_id, participant_a_id, participant_b_id)
);

CREATE INDEX ix_pairing_exclusion_event_id ON pairing_exclusion (event_id);
CREATE INDEX ix_pairing_exclusion_participant_a_id ON pairing_exclusion (participant_a_id);
CREATE INDEX ix_pairing_exclusion_participant_b_id ON pairing_exclusion (participant_b_id);
