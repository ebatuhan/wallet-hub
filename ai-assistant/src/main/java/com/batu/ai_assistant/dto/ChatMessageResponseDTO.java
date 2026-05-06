package com.batu.ai_assistant.dto;

import java.time.Instant;

public record ChatMessageResponseDTO(
        MessageRole role,
        String content,
        Instant createdAt) {
}
