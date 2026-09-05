ALTER TABLE invitation
    ADD COLUMN fallback_code VARCHAR(10);

ALTER TABLE invitation
    ADD CONSTRAINT uq_invitation_fallback_code UNIQUE (fallback_code);
