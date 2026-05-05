package com.batu.ai_assistant.dto;

import java.time.Instant;
import java.util.UUID;

import com.batu.ai_assistant.entity.MessageRole;

public record ChatMessageResponseDTO(
        UUID messageId,
        MessageRole role,
        String content,
        Instant createdAt) {
}
