CREATE TABLE IF NOT EXISTS accounts (
    account_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    connection_id UUID NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(255) NOT NULL,
    account_subtype VARCHAR(255),
    account_mask VARCHAR(255) NOT NULL,
    current_balance NUMERIC(19, 2) NOT NULL,
    available_balance NUMERIC(19, 2) NOT NULL,
    iso_currency_code VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE IF EXISTS accounts ADD COLUMN IF NOT EXISTS institution_name VARCHAR(255);
ALTER TABLE IF EXISTS accounts ADD COLUMN IF NOT EXISTS connection_id UUID;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS sync_version;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS institution_logo_url;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS institution_id;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS external_id;

CREATE TABLE IF NOT EXISTS inbox_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
    outbox_event_id UUID PRIMARY KEY,
    routing_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE IF EXISTS outbox_events DROP COLUMN IF EXISTS aggregate_version;
