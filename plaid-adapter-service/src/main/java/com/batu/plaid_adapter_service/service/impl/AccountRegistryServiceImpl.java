package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.exception.DuplicateConnectionException;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.service.AccountRegistryService;

@Service
public class AccountRegistryServiceImpl implements AccountRegistryService {

    private final AccountRegistryRepository accountRegistryRepository;

    public AccountRegistryServiceImpl(AccountRegistryRepository accountRegistryRepository) {
        this.accountRegistryRepository = accountRegistryRepository;
    }

    @Override
    public boolean existsByFingerprintIn(List<String> fingerprints) {
        return accountRegistryRepository.existsByFingerprintIn(fingerprints);
    }

    @Override
    @Transactional
    public AccountRegistry registerAccount(UUID connectionId, UUID accountId, String fingerprint) {
        AccountRegistry existing = accountRegistryRepository.findByAccountId(accountId).orElse(null);
        if (existing != null) {
            return existing;
        }

        if (accountRegistryRepository.findByFingerprint(fingerprint).isPresent()) {
            throw new DuplicateConnectionException("This connection already exists. Remove the current one before continue.");
        }

        return accountRegistryRepository.save(new AccountRegistry(connectionId, accountId, fingerprint));
    }

    @Override
    public List<AccountRegistry> findAccountsByConnection(UUID connectionId) {
        return accountRegistryRepository.findByConnectionId(connectionId);
    }
}
