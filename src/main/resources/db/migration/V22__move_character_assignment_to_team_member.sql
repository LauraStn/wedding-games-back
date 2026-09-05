ALTER TABLE team DROP COLUMN character_id;

ALTER TABLE team_member
    ADD COLUMN character_id UUID REFERENCES game_character (id) ON DELETE SET NULL;

ALTER TABLE team_member
    ADD CONSTRAINT uq_team_member_character UNIQUE (character_id);
