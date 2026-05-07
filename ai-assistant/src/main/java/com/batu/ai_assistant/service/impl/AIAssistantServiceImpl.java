package com.batu.ai_assistant.service.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import io.micrometer.observation.annotation.Observed;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.service.AIAssistantService;
import com.batu.ai_assistant.util.AssistantResponseSanitizer;

@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private final ChatClient chatClient;
    private final AssistantResponseSanitizer assistantResponseSanitizer;
    private final String modelName;
    private final String keepAlive;
    private final String thinkingMode;
    private final Integer numCtx;
    private final Integer numPredict;
    private final Double temperature;
    private final Integer seed;

    public AIAssistantServiceImpl(ChatClient assistantChatClient,
            AssistantResponseSanitizer assistantResponseSanitizer,
            @Value("${spring.ai.ollama.chat.options.model:}") String modelName,
            @Value("${spring.ai.ollama.chat.options.keep-alive:${assistant.ollama.keep-alive:30m}}") String keepAlive,
            @Value("${assistant.ollama.thinking-mode:ENABLED}") String thinkingMode,
            @Value("${spring.ai.ollama.chat.options.num-ctx:#{null}}") Integer numCtx,
            @Value("${spring.ai.ollama.chat.options.num-predict:#{null}}") Integer numPredict,
            @Value("${spring.ai.ollama.chat.options.temperature:0.2}") Double temperature,
            @Value("${spring.ai.ollama.chat.options.seed:7}") Integer seed) {
        this.chatClient = assistantChatClient;
        this.assistantResponseSanitizer = assistantResponseSanitizer;
        this.modelName = modelName;
        this.keepAlive = keepAlive;
        this.thinkingMode = thinkingMode;
        this.numCtx = numCtx;
        this.numPredict = numPredict;
        this.temperature = temperature;
        this.seed = seed;
    }

    @Override
    @Observed(name = "assistant.chat", contextualName = "assistant chat")
    public ChatResponseDTO chat(ChatRequestDTO request, Jwt principal) {
        requireModel();
        String conversationId = principal.getSubject();

        try {
            String response = chatClient
                    .prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(request.message())
                    .options(runtimeOptions())
                    .call()
                    .content();
            String sanitizedResponse = assistantResponseSanitizer.sanitize(response);
            if (sanitizedResponse == null || sanitizedResponse.isBlank()) {
                String safeError = "AI model returned an empty response. Please try again later.";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, safeError);
            }

            return new ChatResponseDTO(sanitizedResponse);
        } catch (TransientAiException | ResourceAccessException exception) {
            String safeError = "AI model is currently unavailable. Please try again later.";
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    safeError,
                    exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        }
    }

    private void requireModel() {
        if (modelName == null || modelName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI model is not configured yet. Add a Spring AI chat model provider later.");
        }
    }

    private OllamaChatOptions runtimeOptions() {
        OllamaChatOptions.Builder builder = OllamaChatOptions.builder()
                .model(modelName)
                .keepAlive(keepAlive)
                .temperature(temperature)
                .seed(seed);

        if (numCtx != null) {
            builder.numCtx(numCtx);
        }
        if (numPredict != null) {
            builder.numPredict(numPredict);
        }

        String normalizedThinkingMode = thinkingMode == null ? "DISABLED" : thinkingMode.trim().toUpperCase();

        switch (normalizedThinkingMode) {
            case "LOW" -> builder.thinkLow();
            case "MEDIUM" -> builder.thinkMedium();
            case "HIGH" -> builder.thinkHigh();
            case "ENABLED" -> builder.enableThinking();
            case "DISABLED" -> builder.disableThinking();
            default -> builder.disableThinking();
        }

        return builder.build();
    }

}
