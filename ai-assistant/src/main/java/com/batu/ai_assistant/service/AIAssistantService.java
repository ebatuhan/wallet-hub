package com.batu.ai_assistant.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.ai_assistant.dto.ChatHistoryResponseDTO;
import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;

public interface AIAssistantService {
    ChatResponseDTO chat(ChatRequestDTO request, Jwt principal);

    ChatHistoryResponseDTO history(Integer limit, String cursor, Jwt principal);
}
