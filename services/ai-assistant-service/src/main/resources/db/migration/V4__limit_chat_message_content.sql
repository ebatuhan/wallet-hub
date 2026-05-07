ALTER TABLE messages
    ADD CONSTRAINT messages_content_length_check
    CHECK (role <> 'USER' OR char_length(content) <= 4000);

UPDATE conversations
SET last_error = substring(last_error from 1 for 500)
WHERE last_error IS NOT NULL AND char_length(last_error) > 500;

ALTER TABLE conversations
    ALTER COLUMN last_error TYPE VARCHAR(500),
    ADD CONSTRAINT conversations_last_error_length_check
    CHECK (last_error IS NULL OR char_length(last_error) <= 500);
