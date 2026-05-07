CREATE TABLE IF NOT EXISTS connection (
    connection_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL UNIQUE,
    access_token VARCHAR(255) NOT NULL,
    institution_id VARCHAR(255) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    error_code VARCHAR(255),
    last_cursor VARCHAR(255),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS version;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS connection_status;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS disabled_at;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS removed_at;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS sync_version;

DROP TABLE IF EXISTS transaction_registry;
DROP TABLE IF EXISTS account_registry;
DROP TABLE IF EXISTS outbox_events;
