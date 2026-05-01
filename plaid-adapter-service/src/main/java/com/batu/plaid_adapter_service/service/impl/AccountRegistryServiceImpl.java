package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.service.AccountRegistryService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class AccountRegistryServiceImpl implements AccountRegistryService {

    private static final Map<String, AccountRegistry> H2_REGISTRIES = new ConcurrentHashMap<>();

    private final AccountRegistryRepository accountRegistryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AccountRegistryServiceImpl(AccountRegistryRepository accountRegistryRepository) {
        this.accountRegistryRepository = accountRegistryRepository;
    }

    @Override
    public AccountRegistry findAccount(UUID connectionId, String externalAccountId) {
        return accountRegistryRepository.findByConnectionIdAndExternalAccountId(connectionId, externalAccountId)
                .orElse(null);
    }

    @Override
    public List<AccountRegistry> findAccountRegistriesByExternalIds(List<String> externalIds) {
        return accountRegistryRepository.findByExternalAccountIdIn(externalIds);
    }

    @Override
    public boolean existsByFingerprintIn(List<String> fingerprints) {
        return accountRegistryRepository.existsByFingerprintIn(fingerprints);
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public AccountRegistry upsertAccount(UUID connectionId, String externalAccountId, UUID accountId) {
        return upsertAccount(connectionId, externalAccountId, accountId, null);
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public AccountRegistry upsertAccount(UUID connectionId, String externalAccountId, UUID accountId, String fingerprint) {
        if (isH2()) {
            return upsertAccountForH2(connectionId, externalAccountId, accountId, fingerprint);
        }

        entityManager.createNativeQuery("""
                insert into account_registry (account_registry_id, connection_id, external_account_id, account_id, fingerprint)
                values (:accountRegistryId, :connectionId, :externalAccountId, :accountId, :fingerprint)
                on conflict (connection_id, external_account_id) do update
                set fingerprint = coalesce(excluded.fingerprint, account_registry.fingerprint)
                """)
                .setParameter("accountRegistryId", UUID.randomUUID())
                .setParameter("connectionId", connectionId)
                .setParameter("externalAccountId", externalAccountId)
                .setParameter("accountId", accountId)
                .setParameter("fingerprint", fingerprint)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        return accountRegistryRepository.findByConnectionIdAndExternalAccountId(connectionId, externalAccountId)
                .orElseThrow();
    }

    private synchronized AccountRegistry upsertAccountForH2(UUID connectionId, String externalAccountId, UUID accountId,
            String fingerprint) {
        String key = connectionId + "|" + externalAccountId;
        AccountRegistry cached = H2_REGISTRIES.get(key);
        if (cached != null) {
            return cached;
        }

        AccountRegistry existing = findAccount(connectionId, externalAccountId);
        if (existing != null) {
            H2_REGISTRIES.put(key, existing);
            return existing;
        }

        AccountRegistry created = accountRegistryRepository.saveAndFlush(
                new AccountRegistry(connectionId, externalAccountId, accountId, fingerprint));
        H2_REGISTRIES.put(key, created);
        return created;
    }

    private boolean isH2() {
        return entityManager.unwrap(Session.class)
                .doReturningWork(connection -> connection.getMetaData().getDatabaseProductName().contains("H2"));
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
}
