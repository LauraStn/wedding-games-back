ALTER TABLE participant DROP CONSTRAINT ck_participant_status;

ALTER TABLE participant
    ADD CONSTRAINT ck_participant_status CHECK (status IN ('INVITED', 'CONFIRMED', 'CONNECTED', 'PAUSED', 'ABSENT', 'DISABLED'));
