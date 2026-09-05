ALTER TABLE answer
    ADD COLUMN controlling_participant_id UUID REFERENCES participant (id) ON DELETE SET NULL;
