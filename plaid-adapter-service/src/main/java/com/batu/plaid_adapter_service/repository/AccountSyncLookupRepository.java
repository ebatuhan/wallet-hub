package com.batu.plaid_adapter_service.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.plaid_adapter_service.entity.AccountSyncLookup;

public interface AccountSyncLookupRepository extends JpaRepository<AccountSyncLookup, UUID> {
    List<AccountSyncLookup> findByPlaidAccountIdIn(Set<String> plaidAccountIds);

    List<AccountSyncLookup> findByConnectionId(UUID connectionId);
}
