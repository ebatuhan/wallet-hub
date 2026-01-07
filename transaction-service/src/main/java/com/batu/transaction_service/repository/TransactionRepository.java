package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);
    Optional<Transaction> findByExternalId(String externalId);
}