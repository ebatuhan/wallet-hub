package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.batu.plaid_adapter_service.TestSupportConfiguration;
import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.enums.ConnectionStatus;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.repository.TransactionRegistryRepository;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountsGetResponse;
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
    private TransactionRegistryRepository transactionRegistryRepository;

    @MockitoBean
    private PlaidClientWrapper plaidClientWrapper;

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @MockitoBean
    private TransactionServiceClient transactionServiceClient;

    @MockitoBean
    private PlaidRequestMapper plaidRequestMapper;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        transactionRegistryRepository.deleteAll();
        accountRegistryRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    @Test
    void syncConnection_allowsOnlyOneConcurrentRun() throws Exception {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-1", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-1");

        Transaction transaction = org.mockito.Mockito.mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn("ext-transaction-1");
        when(transaction.getAccountId()).thenReturn("ext-account-1");
        when(transaction.getPendingTransactionId()).thenReturn(null);

        AccountsGetResponse accountsGetResponse = org.mockito.Mockito.mock(AccountsGetResponse.class);
        when(accountsGetResponse.getAccounts()).thenReturn(List.of(account));

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAdded()).thenReturn(List.of(transaction));
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getRemoved()).thenReturn(Collections.<RemovedTransaction>emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-1");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        AccountRequestDto accountRequest = new AccountRequestDto(UUID.randomUUID(), connection.getUserId(), "Test Bank",
                "Checking", "depository", "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "USD", true);
        TransactionRequestDto transactionRequest = new TransactionRequestDto(UUID.randomUUID(), connection.getUserId(),
                accountRequest.getAccountId(), java.math.BigDecimal.ONE, "USD", "Coffee", "place",
                java.time.LocalDate.now(), false, "in store", "FOOD_AND_DRINK_COFFEE", true);

        when(plaidRequestMapper.toAccountRequest(any(), any(), any())).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionRequest(any(), any(), any(), any())).thenReturn(transactionRequest);
        when(plaidClientWrapper.syncTransactions(any())).thenReturn(transactionsSyncResponse);
        when(accountServiceClient.create(any())).thenReturn(ResponseEntity.noContent().build());
        when(accountServiceClient.update(any(), any())).thenReturn(ResponseEntity.noContent().build());
        when(transactionServiceClient.create(any())).thenReturn(ResponseEntity.noContent().build());
        when(transactionServiceClient.update(any(), any())).thenReturn(ResponseEntity.noContent().build());

        CountDownLatch firstAccountsCallEntered = new CountDownLatch(1);
        CountDownLatch releaseAccountsCall = new CountDownLatch(1);
        when(plaidClientWrapper.accountsGet(any())).thenAnswer(invocation -> {
            firstAccountsCallEntered.countDown();
            releaseAccountsCall.await(5, TimeUnit.SECONDS);
            return accountsGetResponse;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> firstSync = executor.submit(() -> plaidIntegrationService.syncConnection(connection.getConnectionId()));
        firstAccountsCallEntered.await(5, TimeUnit.SECONDS);
        Future<?> secondSync = executor.submit(() -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        releaseAccountsCall.countDown();
        firstSync.get(5, TimeUnit.SECONDS);
        secondSync.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        verify(accountServiceClient, times(1)).create(any());
        verify(transactionServiceClient, times(1)).create(any());
        verify(accountServiceClient, never()).update(any(), any());
        verify(transactionServiceClient, never()).update(any(), any());
        assertEquals(1, accountRegistryRepository.count());
        assertEquals(1, transactionRegistryRepository.count());
        assertEquals("cursor-1", connectionRepository.findById(connection.getConnectionId()).orElseThrow().getLastCursor());
        assertEquals(ConnectionStatus.ACTIVE.name(),
                connectionRepository.findById(connection.getConnectionId()).orElseThrow().getConnectionStatus());
    }

    @Test
    void syncConnection_deletesAccountRegistryWhenAccountCreateFails() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-2", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-2");

        AccountsGetResponse accountsGetResponse = org.mockito.Mockito.mock(AccountsGetResponse.class);
        when(accountsGetResponse.getAccounts()).thenReturn(List.of(account));

        when(plaidClientWrapper.accountsGet(any())).thenReturn(accountsGetResponse);
        when(plaidRequestMapper.toAccountRequest(any(), any(), any())).thenReturn(
                new AccountRequestDto(UUID.randomUUID(), connection.getUserId(), "Test Bank", "Checking", "depository",
                        "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, "USD", true));
        when(accountServiceClient.create(any())).thenThrow(new RuntimeException("account create failed"));

        assertThrows(RuntimeException.class, () -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        assertEquals(0, accountRegistryRepository.count());
        assertEquals(ConnectionStatus.ACTIVE.name(),
                connectionRepository.findById(connection.getConnectionId()).orElseThrow().getConnectionStatus());
        verify(plaidClientWrapper, never()).syncTransactions(any());
    }

    @Test
    void syncConnection_deletesTransactionRegistryWhenTransactionCreateFails() {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-sync-3", "access-token", "ins-1", "Test Bank"));

        AccountBase account = org.mockito.Mockito.mock(AccountBase.class);
        when(account.getAccountId()).thenReturn("ext-account-3");

        Transaction transaction = org.mockito.Mockito.mock(Transaction.class);
        when(transaction.getTransactionId()).thenReturn("ext-transaction-3");
        when(transaction.getAccountId()).thenReturn("ext-account-3");
        when(transaction.getPendingTransactionId()).thenReturn(null);

        AccountsGetResponse accountsGetResponse = org.mockito.Mockito.mock(AccountsGetResponse.class);
        when(accountsGetResponse.getAccounts()).thenReturn(List.of(account));

        TransactionsSyncResponse transactionsSyncResponse = org.mockito.Mockito.mock(TransactionsSyncResponse.class);
        when(transactionsSyncResponse.getAdded()).thenReturn(List.of(transaction));
        when(transactionsSyncResponse.getModified()).thenReturn(Collections.emptyList());
        when(transactionsSyncResponse.getRemoved()).thenReturn(Collections.<RemovedTransaction>emptyList());
        when(transactionsSyncResponse.getNextCursor()).thenReturn("cursor-2");
        when(transactionsSyncResponse.getHasMore()).thenReturn(false);

        AccountRequestDto accountRequest = new AccountRequestDto(UUID.randomUUID(), connection.getUserId(), "Test Bank",
                "Checking", "depository", "checking", "0000", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                "USD", true);
        TransactionRequestDto transactionRequest = new TransactionRequestDto(UUID.randomUUID(), connection.getUserId(),
                accountRequest.getAccountId(), java.math.BigDecimal.ONE, "USD", "Coffee", "place",
                java.time.LocalDate.now(), false, "in store", "FOOD_AND_DRINK_COFFEE", true);

        when(plaidClientWrapper.accountsGet(any())).thenReturn(accountsGetResponse);
        when(plaidClientWrapper.syncTransactions(any())).thenReturn(transactionsSyncResponse);
        when(plaidRequestMapper.toAccountRequest(any(), any(), any())).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionRequest(any(), any(), any(), any())).thenReturn(transactionRequest);
        when(accountServiceClient.create(any())).thenReturn(ResponseEntity.noContent().build());
        when(transactionServiceClient.create(any())).thenThrow(new RuntimeException("transaction create failed"));

        assertThrows(RuntimeException.class, () -> plaidIntegrationService.syncConnection(connection.getConnectionId()));

        assertEquals(1, accountRegistryRepository.count());
        assertEquals(0, transactionRegistryRepository.count());
        assertEquals(ConnectionStatus.ACTIVE.name(),
                connectionRepository.findById(connection.getConnectionId()).orElseThrow().getConnectionStatus());
    }
}
