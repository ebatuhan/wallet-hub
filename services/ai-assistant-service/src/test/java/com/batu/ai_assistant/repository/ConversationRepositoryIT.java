package com.batu.ai_assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.dto.MessageRole;
import com.batu.ai_assistant.entity.Conversation;
import com.batu.ai_assistant.entity.Message;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(ConversationRepositoryIT.PostgreSqlTestcontainersConfiguration.class)
class ConversationRepositoryIT {

    private static final UUID USER_ID = UUID.fromString("8d000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("8d000000-0000-0000-0000-000000000002");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearPersistenceContext() {
        entityManager.clear();
    }

    @Test
    void flywayMigrations_shouldCreateConversationStateAndSpringAiMemoryTables() {
        String conversationTable = jdbcTemplate.queryForObject("select to_regclass('public.conversations')", String.class);
        String messagesTable = jdbcTemplate.queryForObject("select to_regclass('public.messages')", String.class);
        String memoryTable = jdbcTemplate.queryForObject("select to_regclass('public.spring_ai_chat_memory')", String.class);

        assertThat(conversationTable).isEqualTo("conversations");
        assertThat(messagesTable).isEqualTo("messages");
        assertThat(memoryTable).isEqualTo("spring_ai_chat_memory");
    }

    @Test
    void findConversationByUserId_whenConversationExists_shouldReturnMatchingConversationOnly() {
        Conversation conversation = new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z"));
        conversation.setStatus(ConversationStatus.PROCESSING);
        conversation.setUpdatedAt(Instant.parse("2026-05-07T10:01:00Z"));
        conversationRepository.saveAndFlush(conversation);
        conversationRepository.saveAndFlush(new Conversation(OTHER_USER_ID, Instant.parse("2026-05-07T10:02:00Z")));
        entityManager.clear();

        Optional<Conversation> result = conversationRepository.findConversationByUserId(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(USER_ID);
        assertThat(result.get().getStatus()).isEqualTo(ConversationStatus.PROCESSING);
    }

    @Test
    void findAllByStatus_whenProcessingConversationsExist_shouldReturnProcessingOnly() {
        Conversation processing = new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z"));
        processing.setStatus(ConversationStatus.PROCESSING);
        processing.setUpdatedAt(Instant.parse("2026-05-07T10:01:00Z"));
        conversationRepository.saveAndFlush(processing);
        Conversation idle = new Conversation(OTHER_USER_ID, Instant.parse("2026-05-07T10:02:00Z"));
        conversationRepository.saveAndFlush(idle);
        entityManager.clear();

        var result = conversationRepository.findAllByStatus(ConversationStatus.PROCESSING);

        assertThat(result).extracting(Conversation::getId).containsExactly(processing.getId());
    }

    @Test
    void findConversationByUserId_whenConversationMissing_shouldReturnEmpty() {
        Optional<Conversation> result = conversationRepository.findConversationByUserId(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void conversations_whenSameUserIdIsInsertedTwice_shouldEnforceUniqueUserConstraint() {
        conversationRepository.saveAndFlush(new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z")));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                conversationRepository.saveAndFlush(new Conversation(USER_ID, Instant.parse("2026-05-07T10:01:00Z")))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void messages_whenConversationIsMissing_shouldRejectNullConversation() {
        Message message = new Message(null, MessageRole.USER, "hello", Instant.parse("2026-05-07T10:00:00Z"));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> messageRepository.saveAndFlush(message)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void messages_whenContentIsNull_shouldRejectRequiredContent() {
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(USER_ID, Instant.parse("2026-05-07T10:00:00Z")));
        Message message = new Message(conversation, MessageRole.USER, null, Instant.parse("2026-05-07T10:01:00Z"));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> messageRepository.saveAndFlush(message)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void conversationMessages_whenKeysetScrolled_shouldReturnOnlyRequestedUserMessagesInStableOrder() {
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(USER_ID, Instant.parse("2026-05-07T09:59:00Z")));
        Conversation otherConversation = conversationRepository.saveAndFlush(new Conversation(OTHER_USER_ID, Instant.parse("2026-05-07T09:59:00Z")));
        Message oldest = saveMessage(conversation, MessageRole.USER, "oldest", "2026-05-07T10:00:00Z");
        Message middle = saveMessage(conversation, MessageRole.ASSISTANT, "middle", "2026-05-07T10:01:00Z");
        Message newest = saveMessage(conversation, MessageRole.USER, "newest", "2026-05-07T10:02:00Z");
        saveMessage(otherConversation, MessageRole.USER, "other user", "2026-05-07T10:03:00Z");
        entityManager.clear();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        Window<Message> firstPage = messageRepository.findBy(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("conversation"), conversation),
                query -> query.sortBy(sort).limit(2).scroll(ScrollPosition.keyset()));

        assertThat(firstPage.getContent()).extracting(Message::getContent)
                .containsExactly("newest", "middle");
        assertThat(firstPage.hasNext()).isTrue();

        Window<Message> secondPage = messageRepository.findBy(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("conversation"), conversation),
                query -> query.sortBy(sort).limit(2).scroll(firstPage.positionAt(firstPage.size() - 1)));

        assertThat(secondPage.getContent()).extracting(Message::getId)
                .containsExactly(oldest.getId());
        assertThat(secondPage.getContent()).extracting(Message::getId)
                .doesNotContain(newest.getId(), middle.getId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    private Message saveMessage(Conversation conversation, MessageRole role, String content, String createdAt) {
        Message message = new Message(conversation, role, content, Instant.parse(createdAt));
        return messageRepository.saveAndFlush(message);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
