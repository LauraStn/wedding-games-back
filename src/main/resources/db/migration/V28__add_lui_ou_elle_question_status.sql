ALTER TABLE lui_ou_elle_question
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE lui_ou_elle_question
    ADD CONSTRAINT ck_lui_ou_elle_question_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'PLAYED'));
