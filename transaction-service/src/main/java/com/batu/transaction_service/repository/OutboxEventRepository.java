package com.batu.transaction_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.batu.transaction_service.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    @Query(value = """
            select *
            from outbox_events
            where published_at is null
            order by created_at asc
            limit 100
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> findPendingForRelay();
}
