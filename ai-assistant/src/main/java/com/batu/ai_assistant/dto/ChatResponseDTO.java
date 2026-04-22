package com.batu.ai_assistant.dto;

import java.util.UUID;

public record ChatResponseDTO(
        UUID conversationId,
        String message
) {}
