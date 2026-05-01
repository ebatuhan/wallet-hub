ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS version;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS connection_status;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS disabled_at;
ALTER TABLE IF EXISTS connection DROP COLUMN IF EXISTS removed_at;

DROP TABLE IF EXISTS transaction_registry;

ALTER TABLE IF EXISTS account_registry DROP COLUMN IF EXISTS external_account_id;
ALTER TABLE IF EXISTS account_registry DROP COLUMN IF EXISTS command_version;
