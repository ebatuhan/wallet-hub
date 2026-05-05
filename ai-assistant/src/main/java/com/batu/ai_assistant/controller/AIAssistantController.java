package com.batu.ai_assistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.dto.ChatMessageResponseDTO;
import com.batu.ai_assistant.service.AIAssistantService;
import com.batu.shared.dto.response.CursorResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/assistant")
public class AIAssistantController {

    private final AIAssistantService aiAssistantService;

    public AIAssistantController(AIAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(
            @Valid @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(aiAssistantService.chat(request, principal));
    }

    @GetMapping("/chat/history")
    public ResponseEntity<CursorResponse<ChatMessageResponseDTO>> history(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(aiAssistantService.history(limit, cursor, principal));
    }
}
