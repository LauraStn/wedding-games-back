ALTER TABLE answer
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE answer
    ADD CONSTRAINT ck_answer_moderation_status CHECK (moderation_status IN ('PENDING', 'ACCEPTED', 'HIDDEN'));
