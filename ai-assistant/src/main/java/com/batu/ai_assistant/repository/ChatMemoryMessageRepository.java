package com.batu.ai_assistant.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.batu.ai_assistant.dto.MessageRole;

@Repository
public class ChatMemoryMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChatMemoryMessage> findPage(String conversationId, Instant before, int limit) {
        if (before == null) {
            String sql = """
                    SELECT content, type, "timestamp"
                    FROM SPRING_AI_CHAT_MEMORY
                    WHERE conversation_id = ?
                      AND type IN ('USER', 'ASSISTANT')
                      AND content IS NOT NULL
                      AND btrim(content) <> ''
                    ORDER BY "timestamp" DESC
                    LIMIT ?
                    """;

            return jdbcTemplate.query(sql, this::mapMessage, conversationId, limit);
        }

        String sql = """
                SELECT content, type, "timestamp"
                FROM SPRING_AI_CHAT_MEMORY
                WHERE conversation_id = ?
                  AND type IN ('USER', 'ASSISTANT')
                  AND "timestamp" < ?
                  AND content IS NOT NULL
                  AND btrim(content) <> ''
                ORDER BY "timestamp" DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, this::mapMessage, conversationId, java.sql.Timestamp.from(before), limit);
    }

    private ChatMemoryMessage mapMessage(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ChatMemoryMessage(
                rs.getString("content"),
                MessageRole.valueOf(rs.getString("type")),
                rs.getTimestamp("timestamp").toInstant());
    }

    public record ChatMemoryMessage(String content, MessageRole role, Instant createdAt) {
    }
}
