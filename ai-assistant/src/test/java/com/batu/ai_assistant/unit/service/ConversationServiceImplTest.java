package com.batu.ai_assistant.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.dto.MessageRole;
import com.batu.ai_assistant.entity.Conversation;
import com.batu.ai_assistant.entity.Message;
import com.batu.ai_assistant.repository.ConversationRepository;
import com.batu.ai_assistant.repository.MessageRepository;
import com.batu.ai_assistant.service.AIAssistantService;
import com.batu.ai_assistant.service.impl.ConversationServiceImpl;
import com.batu.shared.cursor.CursorUtils;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AIAssistantService aiAssistantService;

    @Mock
    private CursorUtils cursorUtils;

    private ConversationServiceImpl conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationServiceImpl(
                conversationRepository,
                messageRepository,
                aiAssistantService,
                cursorUtils);
    }

    @Test
    void postMessage_whenAiResponds_shouldPersistUserAndAssistantMessagesAndMarkIdle() {
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        when(conversationRepository.findConversationByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.saveAndFlush(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiAssistantService.chat(any(ChatRequestDTO.class), any(Jwt.class)))
                .thenReturn(new ChatResponseDTO("Track discretionary spending weekly."));
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        var response = conversationService.postMessage(new ChatRequestDTO("How can I save more?"), jwt(USER_ID.toString()));

        assertThat(response.message()).isEqualTo("Track discretionary spending weekly.");
        verify(conversationRepository).saveAndFlush(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getStatus()).isEqualTo(ConversationStatus.IDLE);
        assertThat(conversationCaptor.getValue().getLastError()).isNull();
        verify(messageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues()).extracting(Message::getRole)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(messageCaptor.getAllValues()).extracting(Message::getContent)
                .containsExactly("How can I save more?", "Track discretionary spending weekly.");
    }

    @Test
    void postMessage_whenConversationIsAlreadyProcessing_shouldRejectWithConflict() {
        Conversation conversation = new Conversation(USER_ID, java.time.Instant.now());
        conversation.setStatus(ConversationStatus.PROCESSING);
        when(conversationRepository.findConversationByUserId(USER_ID)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.postMessage(new ChatRequestDTO("Hello"), jwt(USER_ID.toString())))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void postMessage_whenAiFails_shouldPersistSafeFailureMessage() {
        when(conversationRepository.findConversationByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.saveAndFlush(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiAssistantService.chat(any(ChatRequestDTO.class), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "AI model is currently unavailable. Please try again later."));
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);

        assertThatThrownBy(() -> conversationService.postMessage(new ChatRequestDTO("Hello"), jwt(USER_ID.toString())))
                .isInstanceOf(ResponseStatusException.class);

        verify(conversationRepository).saveAndFlush(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getStatus()).isEqualTo(ConversationStatus.FAILED);
        assertThat(conversationCaptor.getValue().getLastError())
                .isEqualTo("AI model is currently unavailable. Please try again later.");
    }

    @Test
    void postMessage_whenConversationLockCannotBeAcquired_shouldRejectWithConflict() {
        when(conversationRepository.findConversationByUserId(USER_ID))
                .thenThrow(new PessimisticLockingFailureException("locked"));

        assertThatThrownBy(() -> conversationService.postMessage(new ChatRequestDTO("Hello"), jwt(USER_ID.toString())))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(409));

        verify(aiAssistantService, never()).chat(any(ChatRequestDTO.class), any(Jwt.class));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void failInterruptedProcessingConversations_shouldMarkOnlyProcessingConversationsFailed() {
        Conversation processing = new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z"));
        processing.setStatus(ConversationStatus.PROCESSING);
        when(conversationRepository.findAllByStatus(ConversationStatus.PROCESSING)).thenReturn(List.of(processing));

        conversationService.failInterruptedProcessingConversations();

        assertThat(processing.getStatus()).isEqualTo(ConversationStatus.FAILED);
        assertThat(processing.getLastError()).isEqualTo("AI response was interrupted. Please try again.");
    }

    @Test
    void history_whenConversationMissing_shouldReturnEmptyIdleHistoryWithoutCreatingConversation() {
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var response = conversationService.history(null, null, jwt(USER_ID.toString()));

        assertThat(response.data()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.status()).isEqualTo(ConversationStatus.IDLE);
        assertThat(response.lastError()).isNull();
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(conversationRepository, never()).saveAndFlush(any(Conversation.class));
        verify(messageRepository, never()).findBy(any(Specification.class), any(Function.class));
    }

    @Test
    void history_whenCursorAndMoreMessagesExist_shouldDecodeCursorAndReturnNextCursor() {
        Conversation conversation = new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z"));
        conversation.setStatus(ConversationStatus.FAILED);
        conversation.setLastError("AI model is currently unavailable. Please try again later.");
        Message message = new Message(conversation, MessageRole.ASSISTANT, "Try weekly reviews.", Instant.parse("2026-05-07T10:01:00Z"));
        ScrollPosition decodedPosition = ScrollPosition.keyset();
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conversation));
        when(cursorUtils.decode("cursor-1")).thenReturn(decodedPosition);
        when(cursorUtils.encode(any(ScrollPosition.class))).thenReturn("cursor-2");
        mockWindow(Window.from(List.of(message), index -> ScrollPosition.keyset(), true));

        var response = conversationService.history(20, "cursor-1", jwt(USER_ID.toString()));

        assertThat(response.data()).singleElement().satisfies(historyMessage -> {
            assertThat(historyMessage.role()).isEqualTo(MessageRole.ASSISTANT);
            assertThat(historyMessage.content()).isEqualTo("Try weekly reviews.");
            assertThat(historyMessage.createdAt()).isEqualTo(Instant.parse("2026-05-07T10:01:00Z"));
        });
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("cursor-2");
        assertThat(response.status()).isEqualTo(ConversationStatus.FAILED);
        assertThat(response.lastError()).isEqualTo("AI model is currently unavailable. Please try again later.");
        verify(cursorUtils).decode("cursor-1");
        verify(cursorUtils).encode(any(ScrollPosition.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void mockWindow(Window<Message> window) {
        doReturn(window).when(messageRepository).findBy(
                any(Specification.class),
                any(Function.class));
    }

    private static Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
