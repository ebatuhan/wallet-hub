package com.batu.plaid_adapter_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.plaid_adapter_service.entity.AccountRegistry;

public interface AccountRegistryRepository extends JpaRepository<AccountRegistry, UUID> {

    Optional<AccountRegistry> findByAccountId(UUID accountId);

    Optional<AccountRegistry> findByFingerprint(String fingerprint);

    List<AccountRegistry> findByConnectionId(UUID connectionId);

    boolean existsByFingerprintIn(List<String> fingerprints);
}
