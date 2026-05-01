package com.batu.plaid_adapter_service.service.impl;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;
import com.batu.plaid_adapter_service.repository.TransactionRegistryRepository;
import com.batu.plaid_adapter_service.service.TransactionRegistryService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TransactionRegistryServiceImpl implements TransactionRegistryService {

    private static final Map<String, TransactionRegistry> H2_REGISTRIES = new ConcurrentHashMap<>();

    private final TransactionRegistryRepository transactionRegistryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TransactionRegistryServiceImpl(TransactionRegistryRepository transactionRegistryRepository) {
        this.transactionRegistryRepository = transactionRegistryRepository;
    }

    @Override
    public TransactionRegistry findTransaction(UUID connectionId, String externalTransactionId) {
        return transactionRegistryRepository
                .findByAccountRegistryConnectionIdAndExternalTransactionId(connectionId, externalTransactionId)
                .orElse(null);
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public TransactionRegistry upsertTransaction(AccountRegistry accountRegistry, String externalTransactionId,
            UUID transactionId) {
        if (isH2()) {
            return upsertTransactionForH2(accountRegistry, externalTransactionId, transactionId);
        }

        entityManager.createNativeQuery("""
                insert into transaction_registry (transaction_registry_id, account_registry_id, external_transaction_id, transaction_id)
                values (:transactionRegistryId, :accountRegistryId, :externalTransactionId, :transactionId)
                on conflict (account_registry_id, external_transaction_id) do nothing
                """)
                .setParameter("transactionRegistryId", UUID.randomUUID())
                .setParameter("accountRegistryId", accountRegistry.getAccountRegistryId())
                .setParameter("externalTransactionId", externalTransactionId)
                .setParameter("transactionId", transactionId)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        return transactionRegistryRepository
                .findByAccountRegistryAccountRegistryIdAndExternalTransactionId(
                        accountRegistry.getAccountRegistryId(), externalTransactionId)
                .orElseThrow();
    }

    private synchronized TransactionRegistry upsertTransactionForH2(AccountRegistry accountRegistry,
            String externalTransactionId, UUID transactionId) {
        String key = accountRegistry.getAccountRegistryId() + "|" + externalTransactionId;
        TransactionRegistry cached = H2_REGISTRIES.get(key);
        if (cached != null) {
            return cached;
        }

        TransactionRegistry existing = transactionRegistryRepository
                .findByAccountRegistryAccountRegistryIdAndExternalTransactionId(
                        accountRegistry.getAccountRegistryId(), externalTransactionId)
                .orElse(null);
        if (existing != null) {
            H2_REGISTRIES.put(key, existing);
            return existing;
        }

        TransactionRegistry created = transactionRegistryRepository.saveAndFlush(
                new TransactionRegistry(accountRegistry, externalTransactionId, transactionId));
        H2_REGISTRIES.put(key, created);
        return created;
    }

    private boolean isH2() {
        return entityManager.unwrap(Session.class)
                .doReturningWork(connection -> connection.getMetaData().getDatabaseProductName().contains("H2"));
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionRegistryId) {
        transactionRegistryRepository.deleteById(transactionRegistryId);
    }
}
