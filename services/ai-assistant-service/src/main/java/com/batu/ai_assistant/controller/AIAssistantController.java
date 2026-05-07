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

import com.batu.ai_assistant.dto.ChatHistoryResponseDTO;
import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/assistant")
@Tag(name = "AI Assistant", description = "AI assistant chat and conversation history endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class AIAssistantController {

    private final ConversationService conversationService;

    public AIAssistantController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Post chat message", description = "Sends a user message to the assistant and returns the assistant response.")
    public ResponseEntity<ChatResponseDTO> chat(
            @Valid @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(conversationService.postMessage(request, principal));
    }

    @GetMapping("/chat/history")
    @Operation(summary = "Get chat history", description = "Returns cursor-paginated assistant conversation history.")
    public ResponseEntity<ChatHistoryResponseDTO> history(
            @RequestParam(defaultValue = "30") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") int limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(conversationService.history(limit, cursor, principal));
    }
}
