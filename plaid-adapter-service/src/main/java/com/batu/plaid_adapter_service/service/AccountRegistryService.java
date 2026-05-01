package com.batu.plaid_adapter_service.service;

import java.util.List;
import java.util.UUID;

import com.batu.plaid_adapter_service.entity.AccountRegistry;

public interface AccountRegistryService {

    AccountRegistry findAccount(UUID connectionId, String externalAccountId);

    List<AccountRegistry> findAccountRegistriesByExternalIds(List<String> externalIds);

    boolean existsByFingerprintIn(List<String> fingerprints);

    AccountRegistry upsertAccount(UUID connectionId, String externalAccountId, UUID accountId);

    AccountRegistry upsertAccount(UUID connectionId, String externalAccountId, UUID accountId, String fingerprint);

    AccountRegistry updateAccountFingerprint(AccountRegistry accountRegistry, String fingerprint);

    List<AccountRegistry> findAccountsByConnection(UUID connectionId);

    void deleteAccount(UUID accountRegistryId);
}
