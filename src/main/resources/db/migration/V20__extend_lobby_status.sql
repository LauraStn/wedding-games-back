ALTER TABLE lobby DROP CONSTRAINT ck_lobby_status;

ALTER TABLE lobby
    ADD CONSTRAINT ck_lobby_status CHECK (status IN ('CLOSED', 'OPEN', 'LOCKED', 'ACTIVE', 'PAUSED', 'FINISHED'));
