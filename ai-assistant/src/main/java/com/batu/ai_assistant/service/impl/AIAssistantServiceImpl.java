package com.batu.ai_assistant.service.impl;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

import com.batu.ai_assistant.dto.ChatMessageResponseDTO;
import com.batu.ai_assistant.dto.ChatHistoryResponseDTO;
import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.repository.ChatMemoryMessageRepository;
import com.batu.ai_assistant.repository.ChatMemoryMessageRepository.ChatMemoryMessage;
import com.batu.ai_assistant.service.AIAssistantService;

@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private final ChatClient chatClient;
    private final ChatMemoryMessageRepository chatMemoryMessageRepository;
    private final String modelName;
    private final String keepAlive;
    private final String thinkingMode;
    private final Integer numCtx;
    private final Integer numPredict;
    private final Double temperature;
    private final Integer seed;

    public AIAssistantServiceImpl(ChatClient assistantChatClient,
            ChatMemoryMessageRepository chatMemoryMessageRepository,
            @Value("${spring.ai.ollama.chat.options.model:}") String modelName,
            @Value("${spring.ai.ollama.chat.options.keep-alive:${assistant.ollama.keep-alive:30m}}") String keepAlive,
            @Value("${assistant.ollama.thinking-mode:ENABLED}") String thinkingMode,
            @Value("${spring.ai.ollama.chat.options.num-ctx:#{null}}") Integer numCtx,
            @Value("${spring.ai.ollama.chat.options.num-predict:#{null}}") Integer numPredict,
            @Value("${spring.ai.ollama.chat.options.temperature:0.2}") Double temperature,
            @Value("${spring.ai.ollama.chat.options.seed:7}") Integer seed) {
        this.chatClient = assistantChatClient;
        this.chatMemoryMessageRepository = chatMemoryMessageRepository;
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
        String conversationId = conversationId(principal);

        try {
            String response = chatClient
                    .prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(request.message())
                    .options(runtimeOptions())
                    .call()
                    .content();
            String sanitizedResponse = sanitizeAssistantResponse(response);

            return new ChatResponseDTO(sanitizedResponse);
        } catch (TransientAiException | ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI model is currently unavailable. Please try again later.",
                    exception);
        }
    }

    @Override
    public ChatHistoryResponseDTO history(Integer limit, String cursor, Jwt principal) {
        int pageSize = Math.min(Math.max(limit == null ? 30 : limit, 1), 100);
        Instant before = decodeCursor(cursor);
        List<ChatMemoryMessage> messages = chatMemoryMessageRepository.findPage(conversationId(principal), before, pageSize + 1);
        boolean hasMore = messages.size() > pageSize;
        List<ChatMemoryMessage> pageMessages = messages.stream()
                .limit(pageSize)
                .toList();
        String nextCursor = hasMore && !pageMessages.isEmpty()
                ? encodeCursor(pageMessages.get(pageMessages.size() - 1).createdAt())
                : null;

        return new ChatHistoryResponseDTO(
                pageMessages.stream().map(this::toMessageResponse).toList(),
                hasMore,
                nextCursor,
                ConversationStatus.IDLE,
                null);
    }

    private ChatMessageResponseDTO toMessageResponse(ChatMemoryMessage message) {
        return new ChatMessageResponseDTO(
                message.role(),
                message.content(),
                message.createdAt());
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

    private String conversationId(Jwt principal) {
        UUID.fromString(principal.getSubject());
        return principal.getSubject();
    }

    private String encodeCursor(Instant createdAt) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(createdAt.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Instant decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid history cursor.");
        }
    }
}
