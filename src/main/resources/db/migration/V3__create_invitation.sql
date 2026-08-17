CREATE TABLE invitation (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_id UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitation_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX ix_invitation_participant_id ON invitation (participant_id);
CREATE INDEX ix_invitation_participant_active
    ON invitation (participant_id)
    WHERE status = 'ACTIVE';
