package com.batu.ai_assistant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import com.batu.ai_assistant.dto.ConversationStatus;
import com.batu.ai_assistant.entity.Conversation;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    Optional<Conversation> findConversationByUserId(UUID userId);

    List<Conversation> findAllByStatus(ConversationStatus status);
}
