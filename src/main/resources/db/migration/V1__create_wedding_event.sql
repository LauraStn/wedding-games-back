CREATE TABLE wedding_event (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          VARCHAR(100) NOT NULL,
    title         VARCHAR(200) NOT NULL,
    language      VARCHAR(10) NOT NULL DEFAULT 'fr-FR',
    status        VARCHAR(20) NOT NULL,
    visual_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wedding_event_slug UNIQUE (slug),
    CONSTRAINT ck_wedding_event_status CHECK (status IN ('DRAFT', 'OPEN', 'LIVE', 'CLOSED'))
);
