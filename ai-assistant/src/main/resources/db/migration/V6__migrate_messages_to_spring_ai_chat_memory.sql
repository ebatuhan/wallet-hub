INSERT INTO SPRING_AI_CHAT_MEMORY (conversation_id, content, type, "timestamp")
SELECT message.conversation_id::VARCHAR(36),
       message.content,
       message.role,
       message.created_at
FROM messages message
WHERE message.role IN ('USER', 'ASSISTANT')
  AND message.content IS NOT NULL
  AND btrim(message.content) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM SPRING_AI_CHAT_MEMORY memory
      WHERE memory.conversation_id = message.conversation_id::VARCHAR(36)
        AND memory.content = message.content
        AND memory.type = message.role
        AND memory."timestamp" = message.created_at
  );
