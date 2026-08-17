CREATE TABLE app_session (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type         VARCHAR(20) NOT NULL,
    participant_id     UUID REFERENCES participant (id) ON DELETE CASCADE,
    staff_account_id   UUID REFERENCES staff_account (id) ON DELETE CASCADE,
    role               VARCHAR(20) NOT NULL,
    session_token_hash VARCHAR(128) NOT NULL,
    last_seen_at       TIMESTAMPTZ NOT NULL,
    expires_at         TIMESTAMPTZ NOT NULL,
    revoked_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_session_token_hash UNIQUE (session_token_hash),
    CONSTRAINT ck_app_session_actor_type CHECK (actor_type IN ('PARTICIPANT', 'STAFF')),
    CONSTRAINT ck_app_session_role CHECK (role IN ('ADMIN', 'INTERVENANT', 'JURY', 'PARTICIPANT', 'PROJECTION')),
    CONSTRAINT ck_app_session_actor_reference CHECK (
        (actor_type = 'PARTICIPANT' AND participant_id IS NOT NULL AND staff_account_id IS NULL)
        OR (actor_type = 'STAFF' AND staff_account_id IS NOT NULL AND participant_id IS NULL)
    )
);

CREATE INDEX ix_app_session_participant_id ON app_session (participant_id);
CREATE INDEX ix_app_session_staff_account_id ON app_session (staff_account_id);
