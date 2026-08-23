ALTER TABLE wedding_event
    ADD COLUMN spouse_one_name VARCHAR(150),
    ADD COLUMN spouse_two_name VARCHAR(150),
    ADD COLUMN event_date DATE,
    ADD COLUMN venue_name VARCHAR(200),
    ADD COLUMN welcome_message VARCHAR(2000);
