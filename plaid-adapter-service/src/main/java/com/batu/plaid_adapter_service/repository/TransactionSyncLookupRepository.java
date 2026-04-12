package com.batu.plaid_adapter_service.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.plaid_adapter_service.entity.TransactionSyncLookup;

public interface TransactionSyncLookupRepository extends JpaRepository<TransactionSyncLookup, UUID> {
    List<TransactionSyncLookup> findByPlaidTransactionIdIn(Set<String> plaidTransactionIds);

    List<TransactionSyncLookup> findByConnectionId(UUID connectionId);
}
