package com.batu.ai_assistant.dto;

import java.util.List;

import com.batu.ai_assistant.entity.ConversationStatus;

public record ChatHistoryResponseDTO(
        List<ChatMessageResponseDTO> data,
        boolean hasMore,
        String nextCursor,
        ConversationStatus status,
        String lastError) {
}
