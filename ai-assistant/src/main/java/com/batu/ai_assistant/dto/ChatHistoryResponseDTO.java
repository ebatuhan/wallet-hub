package com.batu.ai_assistant.dto;

import java.util.List;

public record ChatHistoryResponseDTO(
        List<ChatMessageResponseDTO> data,
        boolean hasMore,
        String nextCursor,
        ConversationStatus status,
        String lastError) {
}
