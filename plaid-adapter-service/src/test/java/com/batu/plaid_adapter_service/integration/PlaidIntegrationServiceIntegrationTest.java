package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.batu.plaid_adapter_service.TestSupportConfiguration;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.messaging.PlaidOutbox;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.ConnectionRemoved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSupportConfiguration.class)
class PlaidIntegrationServiceIntegrationTest {

    @Autowired
    private PlaidIntegrationService plaidIntegrationService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private AccountRegistryRepository accountRegistryRepository;

    @Autowired
    private DeterministicIdGenerator deterministicIdGenerator;


    @MockitoBean
    private PlaidClientWrapper plaidClientWrapper;

    @MockitoBean
    private PlaidRequestMapper plaidRequestMapper;

    @MockitoBean
    private PlaidOutbox plaidOutbox;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        accountRegistryRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    @Test
    void syncConnection_serializesConcurrentRunsForSameConnection() throws Exception {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-1", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-1");

        Transaction transaction = org.mockito.Mockito.mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn("ext-transaction-1");
        when(transaction.getAccountId()).thenReturn("ext-account-1");
        when(transaction.getPendingTransactionId()).thenReturn(null);

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAccounts()).thenReturn(List.of(account));
        when(transactionsSyncResponse.getAdded()).thenReturn(List.of(transaction));
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getRemoved()).thenReturn(Collections.<RemovedTransaction>emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-1");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        AccountObserved accountRequest = new AccountObserved(UUID.randomUUID(), connection.getUserId(), connection.getConnectionId(), "Test Bank",
                "Checking", "depository", "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "USD");
        TransactionObserved transactionRequest = new TransactionObserved(UUID.randomUUID(), connection.getUserId(),
                accountRequest.getAccountId(), java.math.BigDecimal.ONE, "USD", "Coffee", "place",
                java.time.LocalDate.now(), false, "in store", "FOOD_AND_DRINK_COFFEE", true);

        when(plaidRequestMapper.toAccountObserved(any(), any(), any())).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionObserved(any(), any(), any(), any())).thenReturn(transactionRequest);
        CountDownLatch firstSyncCallEntered = new CountDownLatch(1);
        CountDownLatch releaseSyncCall = new CountDownLatch(1);
        when(plaidClientWrapper.syncTransactions(any())).thenAnswer(invocation -> {
            firstSyncCallEntered.countDown();
            releaseSyncCall.await(5, TimeUnit.SECONDS);
            return transactionsSyncResponse;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> firstSync = executor.submit(() -> plaidIntegrationService.syncConnection(connection.getConnectionId()));
        firstSyncCallEntered.await(5, TimeUnit.SECONDS);
        Future<?> secondSync = executor.submit(() -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        releaseSyncCall.countDown();
        firstSync.get(5, TimeUnit.SECONDS);
        secondSync.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        verify(plaidOutbox, times(2)).accountObserved(any(), anyLong());
        verify(plaidOutbox, times(2)).transactionObserved(any(), anyLong());
        assertEquals(1, accountRegistryRepository.count());
        assertEquals("cursor-1", connectionRepository.findById(connection.getConnectionId()).orElseThrow().getLastCursor());
        assertEquals(2, connectionRepository.findById(connection.getConnectionId()).orElseThrow().getSyncVersion());
        assertEquals(true, connectionRepository.findById(connection.getConnectionId()).orElseThrow().isActive());
    }

    @Test
    void syncConnection_deletesAccountRegistryWhenAccountCreateFails() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-2", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-2");

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAccounts()).thenReturn(List.of(account));
        when(transactionsSyncResponse.getAdded()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-failed-account");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        when(plaidClientWrapper.syncTransactions(any())).thenReturn(transactionsSyncResponse);
        when(plaidRequestMapper.toAccountObserved(any(), any(), any())).thenReturn(
                new AccountObserved(UUID.randomUUID(), connection.getUserId(), connection.getConnectionId(), "Test Bank", "Checking", "depository",
                        "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, "USD"));
        doThrow(new RuntimeException("account publish failed")).when(plaidOutbox).accountObserved(any(), anyLong());

        assertThrows(RuntimeException.class, () -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        assertEquals(0, accountRegistryRepository.count());
        assertEquals(true, connectionRepository.findById(connection.getConnectionId()).orElseThrow().isActive());
        verify(plaidOutbox, never()).transactionObserved(any(), anyLong());
    }

    @Test
    void syncConnection_rollsBackAccountRegistryWhenTransactionPublishFails() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-3", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-3");

        Transaction transaction = org.mockito.Mockito.mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn("ext-transaction-3");
        when(transaction.getAccountId()).thenReturn("ext-account-3");
        when(transaction.getPendingTransactionId()).thenReturn(null);

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAccounts()).thenReturn(List.of(account));
        when(transactionsSyncResponse.getAdded()).thenReturn(List.of(transaction));
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getRemoved()).thenReturn(Collections.<RemovedTransaction>emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-2");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        AccountObserved accountRequest = new AccountObserved(UUID.randomUUID(), connection.getUserId(), connection.getConnectionId(), "Test Bank",
                "Checking", "depository", "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "USD");
        TransactionObserved transactionRequest = new TransactionObserved(UUID.randomUUID(), connection.getUserId(),
                accountRequest.getAccountId(), java.math.BigDecimal.ONE, "USD", "Coffee", "place",
                java.time.LocalDate.now(), false, "in store", "FOOD_AND_DRINK_COFFEE", true);

        when(plaidClientWrapper.syncTransactions(any())).thenReturn(transactionsSyncResponse);
        when(plaidRequestMapper.toAccountObserved(any(), any(), any())).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionObserved(any(), any(), any(), any())).thenReturn(transactionRequest);
        doThrow(new RuntimeException("transaction publish failed")).when(plaidOutbox).transactionObserved(any(), anyLong());

        assertThrows(RuntimeException.class, () -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        assertEquals(0, accountRegistryRepository.count());
        assertEquals(true, connectionRepository.findById(connection.getConnectionId()).orElseThrow().isActive());
    }

    @Test
    void syncConnection_publishesDeterministicAccountAndTransactionIds() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-4", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-4");

        Transaction transaction = org.mockito.Mockito.mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn("ext-transaction-4");
        when(transaction.getAccountId()).thenReturn("ext-account-4");
        when(transaction.getPendingTransactionId()).thenReturn(null);

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAccounts()).thenReturn(List.of(account));
        when(transactionsSyncResponse.getAdded()).thenReturn(List.of(transaction));
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getRemoved()).thenReturn(Collections.<RemovedTransaction>emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-4");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        AccountObserved accountRequest = new AccountObserved(UUID.randomUUID(), connection.getUserId(), connection.getConnectionId(), "Test Bank",
                "Checking", "depository", "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "USD");
        TransactionObserved transactionRequest = new TransactionObserved(UUID.randomUUID(), connection.getUserId(),
                accountRequest.getAccountId(), java.math.BigDecimal.ONE, "USD", "Coffee", "place",
                java.time.LocalDate.now(), false, "in store", "FOOD_AND_DRINK_COFFEE", true);

        when(plaidClientWrapper.syncTransactions(any())).thenReturn(transactionsSyncResponse);
        when(plaidRequestMapper.toAccountObserved(any(), any(), any())).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionObserved(any(), any(), any(), any())).thenReturn(transactionRequest);

        plaidIntegrationService.syncConnection(connection.getConnectionId());

        UUID expectedAccountId = deterministicIdGenerator.accountId(connection.getUserId(), "ext-account-4");
        UUID expectedTransactionId = deterministicIdGenerator.transactionId(
                connection.getUserId(), "ext-account-4", "ext-transaction-4");

        ArgumentCaptor<UUID> accountIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> transactionIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> transactionAccountIdCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(plaidRequestMapper).toAccountObserved(any(), accountIdCaptor.capture(), any());
        verify(plaidRequestMapper).toTransactionObserved(any(), transactionIdCaptor.capture(), transactionAccountIdCaptor.capture(), any());

        assertEquals(expectedAccountId, accountIdCaptor.getValue());
        assertEquals(expectedTransactionId, transactionIdCaptor.getValue());
        assertEquals(expectedAccountId, transactionAccountIdCaptor.getValue());
    }

    @Test
    void removeConnection_publishesConnectionRemovedWithIncrementedVersion() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-remove-1", "access-token", "ins-1", "Test Bank"));
        UUID accountId = UUID.randomUUID();
        accountRegistryRepository.save(new AccountRegistry(
                connection.getConnectionId(), accountId, "fingerprint-remove-1"));

        plaidIntegrationService.removeConnection(connection.getConnectionId(), "USER_REQUESTED_REMOVAL");

        ArgumentCaptor<ConnectionRemoved> connectionRemovedCaptor = ArgumentCaptor.forClass(ConnectionRemoved.class);
        ArgumentCaptor<Long> versionCaptor = ArgumentCaptor.forClass(Long.class);

        verify(plaidOutbox).connectionRemoved(connectionRemovedCaptor.capture(), versionCaptor.capture());
        verify(plaidClientWrapper).removeItem(any());

        assertEquals(connection.getConnectionId(), connectionRemovedCaptor.getValue().getConnectionId());
        assertEquals(connection.getUserId(), connectionRemovedCaptor.getValue().getUserId());
        assertEquals(1, versionCaptor.getValue());

        Connection removedConnection = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertEquals(false, removedConnection.isActive());
        assertEquals(1, removedConnection.getSyncVersion());
    }
}
