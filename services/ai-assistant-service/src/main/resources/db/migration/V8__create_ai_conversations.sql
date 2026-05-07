CREATE TABLE IF NOT EXISTS conversations (
    conversation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('IDLE', 'PROCESSING', 'FAILED')),
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    message_id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS messages_conversation_id_created_at_id_idx
ON messages(conversation_id, created_at DESC, message_id ASC);
