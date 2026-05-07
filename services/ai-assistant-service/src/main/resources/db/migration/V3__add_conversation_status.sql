ALTER TABLE conversations
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    ADD COLUMN processing_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_error TEXT;
