package com.batu.ai_assistant.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.ai_assistant.entity.Conversation;
import com.batu.ai_assistant.entity.Message;
import com.batu.ai_assistant.entity.MessageRole;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    List<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);

    List<Message> findByConversationAndRoleInOrderByCreatedAtDesc(
            Conversation conversation,
            List<MessageRole> roles,
            Pageable pageable);

    List<Message> findByConversationAndRoleInAndCreatedAtBeforeOrderByCreatedAtDesc(
            Conversation conversation,
            List<MessageRole> roles,
            java.time.Instant createdAt,
            Pageable pageable);

    boolean existsByConversationAndRoleAndContent(Conversation conversation, MessageRole role, String content);

    void deleteByConversation(Conversation conversation);
}
