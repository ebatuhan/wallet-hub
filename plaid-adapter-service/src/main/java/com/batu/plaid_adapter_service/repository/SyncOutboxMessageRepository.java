package com.batu.plaid_adapter_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.plaid_adapter_service.entity.SyncOutboxMessage;
import com.batu.plaid_adapter_service.entity.enums.SyncOutboxMessageStatus;

import jakarta.persistence.LockModeType;

public interface SyncOutboxMessageRepository extends JpaRepository<SyncOutboxMessage, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message
            from SyncOutboxMessage message
            where message.status = :status
            order by message.createdAt asc
            """)
    List<SyncOutboxMessage> findByStatusOrdered(@Param("status") SyncOutboxMessageStatus status, Pageable pageable);
}
