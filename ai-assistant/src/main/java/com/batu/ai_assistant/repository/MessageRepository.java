package com.batu.ai_assistant.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.batu.ai_assistant.entity.Message;

public interface MessageRepository extends JpaRepository<Message, UUID>, JpaSpecificationExecutor<Message> {
}
