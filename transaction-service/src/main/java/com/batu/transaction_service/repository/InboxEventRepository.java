package com.batu.transaction_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.transaction_service.entity.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
}
