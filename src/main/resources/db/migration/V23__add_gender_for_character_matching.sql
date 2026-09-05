ALTER TABLE participant
    ADD COLUMN gender VARCHAR(10),
    ADD CONSTRAINT ck_participant_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));

ALTER TABLE game_character
    ADD COLUMN gender VARCHAR(10),
    ADD CONSTRAINT ck_game_character_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
