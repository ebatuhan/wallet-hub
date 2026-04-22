package com.batu.ai_assistant.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.exception.ModelNotConfiguredException;
import com.batu.ai_assistant.service.AIAssistantService;

@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private final ChatClient chatClient;
    private final String modelName;
    private final String keepAlive;
    private final String thinkingMode;

    public AIAssistantServiceImpl(ChatClient assistantChatClient,
            @Value("${spring.ai.ollama.chat.options.model:}") String modelName,
            @Value("${assistant.ollama.keep-alive:30m}") String keepAlive,
            @Value("${assistant.ollama.thinking-mode:ENABLED}") String thinkingMode) {
        this.chatClient = assistantChatClient;
        this.modelName = modelName;
        this.keepAlive = keepAlive;
        this.thinkingMode = thinkingMode;
    }

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, Jwt principal) {
        if (modelName == null || modelName.isBlank()) {
            throw new ModelNotConfiguredException();
        }

        UUID conversationId = request.conversationId() == null ? UUID.randomUUID() : request.conversationId();

        String response = chatClient
                .prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                .user(request.message())
                .options(runtimeOptions())
                .toolContext(Map.of("authorization", "Bearer " + principal.getTokenValue()))
                .call()
                .content();


        

        return new ChatResponseDTO(conversationId, sanitizeAssistantResponse(response));
    }

    private OllamaChatOptions.Builder runtimeOptions() {
        OllamaChatOptions.Builder builder = OllamaChatOptions.builder()
                .model(modelName)
                .keepAlive(keepAlive)
                .enableThinking()
                .temperature(0.2)
                .seed(7);

                /*
                
                
                String normalizedThinkingMode = thinkingMode == null ? "ENABLED" : thinkingMode.trim().toUpperCase();
        
                switch (normalizedThinkingMode) {
                    case "DISABLED" -> builder.disableThinking();
                    case "LOW" -> builder.thinkLow();
                    case "MEDIUM" -> builder.thinkMedium();
                    case "HIGH" -> builder.thinkHigh();
                    case "ENABLED" -> builder.enableThinking();
                    default -> builder.enableThinking();
                }
                */

        return builder;
    }

    private String sanitizeAssistantResponse(String response) {
        if (response == null) {
            return null;
        }

        return response
                .replaceAll("\\(ID:\\s*[0-9a-fA-F-]{36}\\)", "")
                .replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "")
                .replaceAll("\\s{2,}", " ")
                .replace(" .", ".")
                .trim();
    }
}
