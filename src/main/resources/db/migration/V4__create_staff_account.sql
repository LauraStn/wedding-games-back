CREATE TABLE staff_account (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(150) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_account_username UNIQUE (username),
    CONSTRAINT ck_staff_account_role CHECK (role IN ('ADMIN', 'INTERVENANT', 'JURY', 'PROJECTION'))
);
