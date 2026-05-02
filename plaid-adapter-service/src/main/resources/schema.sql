ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS version;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS connection_status;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS disabled_at;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS removed_at;

DROP TABLE IF EXISTS transaction_registry;

ALTER TABLE IF EXISTS account_registry DROP COLUMN IF EXISTS external_account_id;
ALTER TABLE IF EXISTS account_registry DROP COLUMN IF EXISTS command_version;

CREATE TABLE IF NOT EXISTS outbox_events (
    outbox_event_id UUID PRIMARY KEY,
    routing_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
