package com.batu.ai_assistant.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.ai_assistant.dto.ChatMessageResponseDTO;
import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.shared.dto.response.CursorResponse;

public interface AIAssistantService {
    ChatResponseDTO chat(ChatRequestDTO request, Jwt principal);

    CursorResponse<ChatMessageResponseDTO> history(Integer limit, String cursor, Jwt principal);
}
