package com.batu.ai_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.dto.ChatResponseDTO;
import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.dto.MessageRole;
import com.batu.ai_assistant.entity.Conversation;
import com.batu.ai_assistant.entity.Message;
import com.batu.ai_assistant.repository.ConversationRepository;
import com.batu.ai_assistant.repository.MessageRepository;
import com.batu.ai_assistant.service.impl.ConversationServiceImpl;
import com.batu.shared.cursor.CursorUtils;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ ConversationServiceImpl.class, ConversationServiceImplIT.ServiceTestConfiguration.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConversationServiceImplIT {

    private static final UUID USER_ID = UUID.fromString("c2000000-0000-0000-0000-000000000001");

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AIAssistantService aiAssistantService;

    @BeforeEach
    void cleanDatabase() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    void postMessage_whenAiResponds_shouldCommitMessagesAndIdleStatusThroughSpringTransaction() {
        when(aiAssistantService.chat(any(ChatRequestDTO.class), any(Jwt.class)))
                .thenReturn(new ChatResponseDTO("Review subscriptions monthly."));

        var response = conversationService.postMessage(new ChatRequestDTO("How can I save?"), jwt(USER_ID));
        entityManager.clear();

        Conversation conversation = conversationRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(response.message()).isEqualTo("Review subscriptions monthly.");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.IDLE);
        assertThat(conversation.getLastError()).isNull();
        assertThat(messageRepository.findAll()).extracting(Message::getRole)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
        assertThat(messageRepository.findAll()).extracting(Message::getContent)
                .containsExactly("How can I save?", "Review subscriptions monthly.");
    }

    @Test
    void postMessage_whenAiFails_shouldCommitUserMessageAndFailedStatusThroughSpringTransaction() {
        when(aiAssistantService.chat(any(ChatRequestDTO.class), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "AI model is currently unavailable. Please try again later."));

        assertThatThrownBy(() -> conversationService.postMessage(new ChatRequestDTO("Hello"), jwt(USER_ID)))
                .isInstanceOf(ResponseStatusException.class);
        entityManager.clear();

        Conversation conversation = conversationRepository.findByUserId(USER_ID).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.FAILED);
        assertThat(conversation.getLastError()).isEqualTo("AI model is currently unavailable. Please try again later.");
        assertThat(messageRepository.findAll()).extracting(Message::getRole)
                .containsExactly(MessageRole.USER);
    }

    @Test
    void postMessage_whenConversationIsAlreadyProcessing_shouldRejectDuplicateWithConflict() {
        Conversation conversation = new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z"));
        conversation.setStatus(ConversationStatus.PROCESSING);
        conversationRepository.saveAndFlush(conversation);
        entityManager.clear();

        assertThatThrownBy(() -> conversationService.postMessage(new ChatRequestDTO("Second"), jwt(USER_ID)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(409));

        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void history_whenMessagesExist_shouldReadPersistedRowsWithConversationStatus() {
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z")));
        conversation.setStatus(ConversationStatus.FAILED);
        conversation.setLastError("AI response was interrupted. Please try again.");
        conversationRepository.saveAndFlush(conversation);
        messageRepository.saveAndFlush(new Message(conversation, MessageRole.USER, "old", Instant.parse("2026-05-07T10:01:00Z")));
        messageRepository.saveAndFlush(new Message(conversation, MessageRole.ASSISTANT, "new", Instant.parse("2026-05-07T10:02:00Z")));
        entityManager.clear();

        var response = conversationService.history(10, null, jwt(USER_ID));

        assertThat(response.data()).extracting(message -> message.content()).containsExactly("new", "old");
        assertThat(response.status()).isEqualTo(ConversationStatus.FAILED);
        assertThat(response.lastError()).isEqualTo("AI response was interrupted. Please try again.");
        assertThat(response.hasMore()).isFalse();
    }

    private static Jwt jwt(UUID subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject.toString())
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ServiceTestConfiguration {

        @Bean
        CursorUtils cursorUtils() {
            return new CursorUtils(new JsonMapper());
        }

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
