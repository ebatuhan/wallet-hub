package com.batu.ai_assistant.dto;

import java.time.Instant;

import com.batu.ai_assistant.entity.MessageRole;

public record ChatMessageResponseDTO(
        MessageRole role,
        String content,
        Instant createdAt) {
}
