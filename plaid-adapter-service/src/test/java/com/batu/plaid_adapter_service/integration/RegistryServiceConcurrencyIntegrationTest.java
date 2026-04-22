package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.batu.plaid_adapter_service.TestSupportConfiguration;
import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.repository.TransactionRegistryRepository;
import com.batu.plaid_adapter_service.service.RegistryService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSupportConfiguration.class)
class RegistryServiceConcurrencyIntegrationTest {

    @Autowired
    private RegistryService registryService;

    @Autowired
    private AccountRegistryRepository accountRegistryRepository;

    @Autowired
    private TransactionRegistryRepository transactionRegistryRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        transactionRegistryRepository.deleteAll();
        accountRegistryRepository.deleteAll();
    }

    @Test
    void createAccount_reusesSingleRegistryRowUnderConcurrency() throws Exception {
        UUID connectionId = UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<AccountRegistry> first = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return registryService.createAccount(connectionId, "ext-account-1", UUID.randomUUID());
        });
        Future<AccountRegistry> second = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return registryService.createAccount(connectionId, "ext-account-1", UUID.randomUUID());
        });

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        AccountRegistry firstResult = first.get(5, TimeUnit.SECONDS);
        AccountRegistry secondResult = second.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertNotNull(firstResult);
        assertNotNull(secondResult);
        assertEquals(firstResult.getAccountRegistryId(), secondResult.getAccountRegistryId());
        assertEquals(1, accountRegistryRepository.count());
    }

    @Test
    void createTransaction_reusesSingleRegistryRowUnderConcurrency() throws Exception {
        AccountRegistry accountRegistry = registryService.createAccount(UUID.randomUUID(), "ext-account-2", UUID.randomUUID());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<TransactionRegistry> first = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return registryService.createTransaction(accountRegistry, "ext-transaction-1", UUID.randomUUID());
        });
        Future<TransactionRegistry> second = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return registryService.createTransaction(accountRegistry, "ext-transaction-1", UUID.randomUUID());
        });

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        TransactionRegistry firstResult = first.get(5, TimeUnit.SECONDS);
        TransactionRegistry secondResult = second.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertNotNull(firstResult);
        assertNotNull(secondResult);
        assertEquals(firstResult.getTransactionRegistryId(), secondResult.getTransactionRegistryId());
        assertEquals(1, transactionRegistryRepository.count());
    }
}
