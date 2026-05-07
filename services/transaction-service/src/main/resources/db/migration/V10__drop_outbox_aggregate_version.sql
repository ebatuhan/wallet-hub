ALTER TABLE IF EXISTS outbox_events
    DROP COLUMN IF EXISTS aggregate_version;
