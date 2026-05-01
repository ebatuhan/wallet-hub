package com.batu.plaid_adapter_service.service;

import java.util.List;
import java.util.UUID;

import com.batu.plaid_adapter_service.entity.AccountRegistry;

public interface AccountRegistryService {

    boolean existsByFingerprintIn(List<String> fingerprints);

    AccountRegistry registerAccount(UUID connectionId, UUID accountId, String fingerprint);

    List<AccountRegistry> findAccountsByConnection(UUID connectionId);
}
