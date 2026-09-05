ALTER TABLE lobby_participant DROP CONSTRAINT ck_lobby_participant_connection_status;

ALTER TABLE lobby_participant
    ADD CONSTRAINT ck_lobby_participant_connection_status
    CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'LATE', 'READY'));
