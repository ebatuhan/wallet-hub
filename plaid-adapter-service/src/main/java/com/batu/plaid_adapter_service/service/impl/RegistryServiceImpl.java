package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.repository.TransactionRegistryRepository;
import com.batu.plaid_adapter_service.service.RegistryService;

@Service
public class RegistryServiceImpl implements RegistryService {

    private final AccountRegistryRepository accountRegistryRepository;
    private final TransactionRegistryRepository transactionRegistryRepository;

    public RegistryServiceImpl(AccountRegistryRepository accountRegistryRepository,
            TransactionRegistryRepository transactionRegistryRepository) {
        this.accountRegistryRepository = accountRegistryRepository;
        this.transactionRegistryRepository = transactionRegistryRepository;
    }

    @Override
    public AccountRegistry findAccount(UUID connectionId, String externalAccountId) {
        return accountRegistryRepository.findByConnectionIdAndExternalAccountId(connectionId, externalAccountId)
                .orElse(null);
    }

    @Override
    @Transactional
    public AccountRegistry createAccount(UUID connectionId, String externalAccountId, UUID accountId) {
        return accountRegistryRepository.save(new AccountRegistry(connectionId, externalAccountId, accountId));
    }

    @Override
    @Transactional
    public AccountRegistry createAccount(UUID connectionId, String externalAccountId, UUID accountId, String fingerprint) {
        return accountRegistryRepository.save(new AccountRegistry(connectionId, externalAccountId, accountId, fingerprint));
    }

    @Override
    @Transactional
    public AccountRegistry updateAccountFingerprint(AccountRegistry accountRegistry, String fingerprint) {
        accountRegistry.setFingerprint(fingerprint);
        return accountRegistryRepository.save(accountRegistry);
    }

    @Override
    public List<AccountRegistry> findAccountsByConnection(UUID connectionId) {
        return accountRegistryRepository.findByConnectionId(connectionId);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountRegistryId) {
        accountRegistryRepository.deleteById(accountRegistryId);
    }

    @Override
    public TransactionRegistry findTransaction(UUID connectionId, String externalTransactionId) {
        return transactionRegistryRepository
                .findByAccountRegistryConnectionIdAndExternalTransactionId(connectionId, externalTransactionId)
                .orElse(null);
    }

    @Override
    @Transactional
    public TransactionRegistry createTransaction(AccountRegistry accountRegistry, String externalTransactionId,
            UUID transactionId) {
        return transactionRegistryRepository
                .save(new TransactionRegistry(accountRegistry, externalTransactionId, transactionId));
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionRegistryId) {
        transactionRegistryRepository.deleteById(transactionRegistryId);
    }
}
