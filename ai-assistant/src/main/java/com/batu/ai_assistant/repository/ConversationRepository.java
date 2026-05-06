package com.batu.ai_assistant.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.ai_assistant.entity.Conversation;

import jakarta.persistence.LockModeType;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conversation from Conversation conversation where conversation.userId = :userId")
    Optional<Conversation> findWithLockByUserId(@Param("userId") UUID userId);
}
