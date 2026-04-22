package com.batu.plaid_adapter_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.plaid_adapter_service.entity.TransactionRegistry;

public interface TransactionRegistryRepository extends JpaRepository<TransactionRegistry, UUID> {

    Optional<TransactionRegistry> findByAccountRegistryConnectionIdAndExternalTransactionId(UUID connectionId,
            String externalTransactionId);
}
