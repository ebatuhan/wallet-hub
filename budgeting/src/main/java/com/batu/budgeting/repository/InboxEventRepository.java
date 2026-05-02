package com.batu.budgeting.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.budgeting.entity.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
}
