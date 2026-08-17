CREATE TABLE participant (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id         UUID NOT NULL REFERENCES wedding_event (id) ON DELETE CASCADE,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    display_name     VARCHAR(150) NOT NULL,
    table_label      VARCHAR(50),
    participant_type VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    total_points     INT NOT NULL DEFAULT 0,
    total_wins       INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_participant_type CHECK (participant_type IN ('GUEST', 'SPOUSE', 'ORGANIZER')),
    CONSTRAINT ck_participant_status CHECK (status IN ('INVITED', 'CONFIRMED', 'CONNECTED', 'PAUSED', 'ABSENT')),
    CONSTRAINT ck_participant_total_points_non_negative CHECK (total_points >= 0),
    CONSTRAINT ck_participant_total_wins_non_negative CHECK (total_wins >= 0)
);

CREATE INDEX ix_participant_event_id ON participant (event_id);
