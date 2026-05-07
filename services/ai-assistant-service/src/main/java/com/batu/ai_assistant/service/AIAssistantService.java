package com.batu.ai_assistant.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;

public interface AIAssistantService {
    ChatResponseDTO chat(ChatRequestDTO request, Jwt principal);
}
