CREATE TABLE IF NOT EXISTS accounts (
    account_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
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
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS institution_logo_url;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS institution_id;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS connection_id;
ALTER TABLE IF EXISTS accounts DROP COLUMN IF EXISTS external_id;
