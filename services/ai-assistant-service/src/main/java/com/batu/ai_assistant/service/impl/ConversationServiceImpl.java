package com.batu.ai_assistant.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.batu.ai_assistant.dto.ChatHistoryResponseDTO;
import com.batu.ai_assistant.dto.ChatMessageResponseDTO;
import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.dto.MessageRole;
import com.batu.ai_assistant.entity.Conversation;
import com.batu.ai_assistant.entity.Message;
import com.batu.ai_assistant.repository.ConversationRepository;
import com.batu.ai_assistant.repository.MessageRepository;
import com.batu.ai_assistant.service.AIAssistantService;
import com.batu.ai_assistant.service.ConversationService;
import com.batu.shared.cursor.CursorUtils;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final String PROCESSING_CONFLICT_MESSAGE = "Assistant is still processing your previous message.";
    private static final String INTERRUPTED_PROCESSING_MESSAGE = "AI response was interrupted. Please try again.";
    private static final String GENERIC_FAILURE_MESSAGE = "AI assistant could not complete the request. Please try again later.";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIAssistantService aiAssistantService;
    private final CursorUtils cursorUtils;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            AIAssistantService aiAssistantService,
            CursorUtils cursorUtils) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiAssistantService = aiAssistantService;
        this.cursorUtils = cursorUtils;
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public ChatResponseDTO postMessage(ChatRequestDTO request, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        Conversation conversation = startProcessing(userId);
        saveMessage(conversation, MessageRole.USER, request.message());

        try {
            ChatResponseDTO response = aiAssistantService.chat(request, principal);
            saveMessage(conversation, MessageRole.ASSISTANT, response.message());
            updateStatus(conversation, ConversationStatus.IDLE, null);
            return response;
        } catch (ResponseStatusException exception) {
            updateStatus(conversation, ConversationStatus.FAILED, safeReason(exception));
            throw exception;
        } catch (RuntimeException exception) {
            updateStatus(conversation, ConversationStatus.FAILED, GENERIC_FAILURE_MESSAGE);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_FAILURE_MESSAGE, exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatHistoryResponseDTO history(Integer limit, String cursor, Jwt principal) {

        UUID userId = UUID.fromString(principal.getSubject());

        int pageSize = Math.min(Math.max(limit == null ? 30 : limit, 1), 100);

        ScrollPosition scrollPosition = cursor == null ? ScrollPosition.keyset() : cursorUtils.decode(cursor);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        Conversation conversation = conversationRepository.findByUserId(userId).orElse(null);

        if (conversation == null) {
            return new ChatHistoryResponseDTO(
                    java.util.List.of(),
                    false,
                    null,
                    ConversationStatus.IDLE,
                    null);
        }

        Window<Message> messages = messageRepository.findBy(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("conversation"), conversation),
                query -> query.sortBy(sort).limit(pageSize).scroll(scrollPosition));

        return new ChatHistoryResponseDTO(
                messages.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                messages.hasNext(),
                messages.hasNext() ? cursorUtils.encode(messages.positionAt(messages.size() - 1)) : null,
                conversation.getStatus(),
                conversation.getLastError());
    }

    private Conversation startProcessing(UUID userId) {
        Instant now = Instant.now();

        Conversation conversation;
        try {
            conversation = conversationRepository.findConversationByUserId(userId)
                    .orElseGet(() -> new Conversation(userId, now));
        } catch (PessimisticLockingFailureException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, PROCESSING_CONFLICT_MESSAGE, exception);
        }

        if (conversation.getStatus() == ConversationStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, PROCESSING_CONFLICT_MESSAGE);
        }

        conversation.setStatus(ConversationStatus.PROCESSING);
        conversation.setLastError(null);
        conversation.setUpdatedAt(now);
        try {
            return conversationRepository.saveAndFlush(conversation);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, PROCESSING_CONFLICT_MESSAGE, exception);
        }
    }

    private void updateStatus(Conversation conversation, ConversationStatus status, String safeError) {
        conversation.setStatus(status);
        conversation.setLastError(safeError);
        conversation.setUpdatedAt(Instant.now());
    }

    @Override
    @Transactional
    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void failInterruptedProcessingConversations() {
        conversationRepository.findAllByStatus(ConversationStatus.PROCESSING)
                .forEach(conversation -> updateStatus(conversation, ConversationStatus.FAILED, INTERRUPTED_PROCESSING_MESSAGE));
    }

    private void saveMessage(Conversation conversation, MessageRole role, String content) {
        messageRepository.save(new Message(conversation, role, content, Instant.now()));
    }

    private ChatMessageResponseDTO toResponse(Message message) {
        return new ChatMessageResponseDTO(message.getRole(), message.getContent(), message.getCreatedAt());
    }

    private String safeReason(ResponseStatusException exception) {
        return exception.getReason() == null || exception.getReason().isBlank()
                ? GENERIC_FAILURE_MESSAGE
                : exception.getReason();
    }
}
