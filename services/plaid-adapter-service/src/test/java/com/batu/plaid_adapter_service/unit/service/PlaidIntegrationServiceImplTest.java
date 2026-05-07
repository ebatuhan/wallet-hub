package com.batu.plaid_adapter_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.plaid_adapter_service.client.AccountClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.impl.PlaidIntegrationServiceImpl;
import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.request.ConnectionAccountMetadataDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountSubtype;
import com.plaid.client.model.AccountType;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;

@ExtendWith(MockitoExtension.class)
class PlaidIntegrationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("77000000-0000-0000-0000-000000000001");
    private static final UUID CONNECTION_ID = UUID.fromString("77000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("77000000-0000-0000-0000-000000000003");
    private static final UUID TRANSACTION_ID = UUID.fromString("77000000-0000-0000-0000-000000000004");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("77000000-0000-0000-0000-000000000005");

    @Mock
    private PlaidClientWrapper plaidClient;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PlaidRequestMapper plaidRequestMapper;

    @Mock
    private DeterministicIdGenerator deterministicIdGenerator;

    private PlaidIntegrationServiceImpl plaidIntegrationService;

    @BeforeEach
    void setUp() {
        plaidIntegrationService = new PlaidIntegrationServiceImpl(
                plaidClient,
                accountClient,
                transactionClient,
                connectionService,
                plaidRequestMapper,
                deterministicIdGenerator);
        ReflectionTestUtils.setField(plaidIntegrationService, "webhookUrl", "https://wallet.example/webhook");
    }

    @Test
    void exchangeLinkToken_whenRequestIsNull_shouldThrowBadRequestAndNotCallPlaid() {
        assertThatThrownBy(() -> plaidIntegrationService.exchangeLinkToken(null, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Exchange token request is required");
                });
        verify(plaidClient, never()).exchangePublicToken(any());
        verify(connectionService, never()).readAllByUserIdAndInstitutionId(any(), any());
    }

    @Test
    void createLinkToken_whenPlaidReturnsToken_shouldBuildPlaidRequestAndReturnToken() {
        ArgumentCaptor<LinkTokenCreateRequest> requestCaptor = ArgumentCaptor.forClass(LinkTokenCreateRequest.class);
        when(plaidClient.createLinkToken(any(LinkTokenCreateRequest.class)))
                .thenReturn(new LinkTokenCreateResponse().linkToken("link-token"));

        var response = plaidIntegrationService.createLinkToken(new LinkTokenRequestDto("US"), USER_ID);

        verify(plaidClient).createLinkToken(requestCaptor.capture());
        LinkTokenCreateRequest plaidRequest = requestCaptor.getValue();
        assertThat(plaidRequest.getUser().getClientUserId()).isEqualTo(USER_ID.toString());
        assertThat(plaidRequest.getClientName()).isEqualTo("Wallet-Hub");
        assertThat(plaidRequest.getLanguage()).isEqualTo("en");
        assertThat(plaidRequest.getWebhook()).isEqualTo("https://wallet.example/webhook");
        assertThat(response.getLinkToken()).isEqualTo("link-token");
    }

    @Test
    void exchangeLinkToken_whenIncomingAccountDuplicatesExistingConnection_shouldThrowConflictAndNotExchangeToken() {
        ExchangeTokenRequestDto request = new ExchangeTokenRequestDto(
                "public-token",
                List.of("account-1"),
                List.of(new ConnectionAccountMetadataDto("account-1", "Checking", " 0000 ", " CHECKING ")),
                "ins-1",
                "Test Bank");
        Connection existingConnection = connection(true);
        when(connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1")).thenReturn(List.of(existingConnection));
        when(accountClient.findAccountsByConnectionId(CONNECTION_ID)).thenReturn(List.of(accountResponse("0000", "checking")));

        assertThatThrownBy(() -> plaidIntegrationService.exchangeLinkToken(request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("This connection already exists. Remove the current one before continue.");
                });
        verify(plaidClient, never()).exchangePublicToken(any());
    }

    @Test
    void exchangeLinkToken_whenRequestIsNotDuplicate_shouldCreateConnectionSyncItAndReturnResponse() {
        ExchangeTokenRequestDto request = new ExchangeTokenRequestDto("public-token", List.of(), List.of(), "ins-1",
                "Test Bank");
        ItemPublicTokenExchangeResponse plaidResponse = new ItemPublicTokenExchangeResponse()
                .itemId("item-1")
                .accessToken("access-token");
        Connection savedConnection = connection(true);
        when(plaidClient.exchangePublicToken(any(ItemPublicTokenExchangeRequest.class))).thenReturn(plaidResponse);
        when(connectionService.create(any(Connection.class))).thenReturn(savedConnection);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(savedConnection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class))).thenReturn(syncResponse("cursor-1", false));

        var response = plaidIntegrationService.exchangeLinkToken(request, USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
        assertThat(response.getInstitutionId()).isEqualTo("ins-1");
        assertThat(response.getInstitutionName()).isEqualTo("Test Bank");
        verify(connectionService).create(any(Connection.class));
        verify(connectionService).completeSync(CONNECTION_ID, "cursor-1");
    }

    @Test
    void exchangeLinkToken_whenAccountsAreNull_shouldSkipDuplicateCheckAndExchangeToken() {
        ExchangeTokenRequestDto request = new ExchangeTokenRequestDto("public-token", "ins-1", "Test Bank");
        request.setAccounts(null);
        stubExchangeAndEmptySync();

        var response = plaidIntegrationService.exchangeLinkToken(request, USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
        verify(connectionService, never()).readAllByUserIdAndInstitutionId(any(), any());
    }

    @Test
    void exchangeLinkToken_whenExistingConnectionsAreEmpty_shouldExchangeToken() {
        ExchangeTokenRequestDto request = duplicateCheckRequest("0000", "checking");
        when(connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1")).thenReturn(List.of());
        stubExchangeAndEmptySync();

        var response = plaidIntegrationService.exchangeLinkToken(request, USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
        verify(accountClient, never()).findAccountsByConnectionId(any());
    }

    @Test
    void exchangeLinkToken_whenExistingAccountsAreEmpty_shouldExchangeToken() {
        ExchangeTokenRequestDto request = duplicateCheckRequest("0000", "checking");
        when(connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1")).thenReturn(List.of(connection(true)));
        when(accountClient.findAccountsByConnectionId(CONNECTION_ID)).thenReturn(List.of());
        stubExchangeAndEmptySync();

        var response = plaidIntegrationService.exchangeLinkToken(request, USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
    }

    @Test
    void exchangeLinkToken_whenIncomingAccountDoesNotMatchExistingAccount_shouldExchangeToken() {
        ExchangeTokenRequestDto request = duplicateCheckRequest("9999", "savings");
        when(connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1")).thenReturn(List.of(connection(true)));
        when(accountClient.findAccountsByConnectionId(CONNECTION_ID)).thenReturn(List.of(accountResponse("0000", "checking")));
        stubExchangeAndEmptySync();

        var response = plaidIntegrationService.exchangeLinkToken(request, USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
    }

    @Test
    void mockToken_whenSandboxTokenCreated_shouldExchangeSandboxPublicToken() {
        when(plaidClient.createSandboxToken(any(SandboxPublicTokenCreateRequest.class)))
                .thenReturn(new SandboxPublicTokenCreateResponse().publicToken("sandbox-public-token"));
        when(plaidClient.exchangePublicToken(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(new ItemPublicTokenExchangeResponse().itemId("item-1").accessToken("access-token"));
        Connection savedConnection = connection(true);
        when(connectionService.create(any(Connection.class))).thenReturn(savedConnection);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(savedConnection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class))).thenReturn(syncResponse("cursor-1", false));

        var response = plaidIntegrationService.mockToken(USER_ID);

        assertThat(response.getConnectionId()).isEqualTo(CONNECTION_ID);
        verify(plaidClient).createSandboxToken(any(SandboxPublicTokenCreateRequest.class));
        verify(plaidClient).exchangePublicToken(any(ItemPublicTokenExchangeRequest.class));
    }

    @Test
    void removeConnection_whenConnectionInactive_shouldReturnWithoutCallingPlaidOrDownstreamServices() {
        Connection connection = connection(false);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection);

        plaidIntegrationService.removeConnection(CONNECTION_ID, "USER_REQUESTED_REMOVAL");

        verify(plaidClient, never()).removeItem(any());
        verify(accountClient, never()).deactivateAccountsByConnection(any());
        verify(connectionService, never()).deactivate(any(), any());
    }

    @Test
    void removeConnection_whenConnectionActive_shouldRemovePlaidItemDeactivateAccountsTransactionsAndConnection() {
        Connection connection = connection(true);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection);
        when(accountClient.deactivateAccountsByConnection(CONNECTION_ID)).thenReturn(List.of(
                accountUpsertResponse(ACCOUNT_ID),
                accountUpsertResponse(OTHER_ACCOUNT_ID)));

        plaidIntegrationService.removeConnection(CONNECTION_ID, "USER_REQUESTED_REMOVAL");

        verify(plaidClient).removeItem(any());
        verify(accountClient).deactivateAccountsByConnection(CONNECTION_ID);
        verify(transactionClient).deactivateTransactionsByAccount(ACCOUNT_ID);
        verify(transactionClient).deactivateTransactionsByAccount(OTHER_ACCOUNT_ID);
        verify(connectionService).deactivate(CONNECTION_ID, "USER_REQUESTED_REMOVAL");
    }

    @Test
    void syncConnection_whenConnectionInactive_shouldReturnWithoutCallingPlaid() {
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection(false));

        plaidIntegrationService.syncConnection(CONNECTION_ID);

        verify(plaidClient, never()).syncTransactions(any());
        verify(accountClient, never()).upsertAccount(any());
        verify(transactionClient, never()).upsertTransaction(any());
    }

    @Test
    void syncConnection_whenSinglePageHasAccountsAndTransactions_shouldUpsertMappedRequestsAndCompleteSync() {
        Connection connection = connection(true);
        AccountBase account = account("plaid-account-1");
        Transaction transaction = transaction("plaid-transaction-1", "plaid-account-1");
        AccountUpsertRequestDto accountRequest = accountRequest();
        TransactionUpsertRequestDto transactionRequest = transactionRequest();
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(new TransactionsSyncResponse()
                        .accounts(List.of(account))
                        .added(List.of(transaction))
                        .modified(List.of())
                        .nextCursor("cursor-1")
                        .hasMore(false));
        when(deterministicIdGenerator.accountId(USER_ID, "plaid-account-1")).thenReturn(ACCOUNT_ID);
        when(deterministicIdGenerator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-1")).thenReturn(TRANSACTION_ID);
        when(plaidRequestMapper.toAccountUpsertRequest(connection, ACCOUNT_ID, account)).thenReturn(accountRequest);
        when(plaidRequestMapper.toTransactionUpsertRequest(connection, TRANSACTION_ID, ACCOUNT_ID, transaction))
                .thenReturn(transactionRequest);

        plaidIntegrationService.syncConnection(CONNECTION_ID);

        verify(accountClient).upsertAccount(accountRequest);
        verify(transactionClient).upsertTransaction(transactionRequest);
        verify(connectionService).completeSync(CONNECTION_ID, "cursor-1");
    }

    @Test
    void syncConnection_whenSinglePageHasModifiedTransactions_shouldUpsertModifiedTransactions() {
        Connection connection = connection(true);
        Transaction transaction = transaction("plaid-transaction-1", "plaid-account-1");
        TransactionUpsertRequestDto transactionRequest = transactionRequest();
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(new TransactionsSyncResponse()
                        .accounts(List.of())
                        .added(List.of())
                        .modified(List.of(transaction))
                        .nextCursor("cursor-1")
                        .hasMore(false));
        when(deterministicIdGenerator.accountId(USER_ID, "plaid-account-1")).thenReturn(ACCOUNT_ID);
        when(deterministicIdGenerator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-1")).thenReturn(TRANSACTION_ID);
        when(plaidRequestMapper.toTransactionUpsertRequest(connection, TRANSACTION_ID, ACCOUNT_ID, transaction))
                .thenReturn(transactionRequest);

        plaidIntegrationService.syncConnection(CONNECTION_ID);

        verify(transactionClient).upsertTransaction(transactionRequest);
        verify(connectionService).completeSync(CONNECTION_ID, "cursor-1");
    }

    @Test
    void syncConnection_whenPlaidHasMultiplePages_shouldContinueUntilLastCursor() {
        Connection connection = connection(true);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(connection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(syncResponse("cursor-page-1", true))
                .thenReturn(syncResponse("cursor-page-2", false));

        plaidIntegrationService.syncConnection(CONNECTION_ID);

        verify(plaidClient, times(2)).syncTransactions(any(TransactionsSyncRequest.class));
        verify(connectionService).completeSync(CONNECTION_ID, "cursor-page-2");
    }

    private Connection connection(boolean active) {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");
        ReflectionTestUtils.setField(connection, "connectionId", CONNECTION_ID);
        connection.setActive(active);
        return connection;
    }

    private TransactionsSyncResponse syncResponse(String cursor, boolean hasMore) {
        return new TransactionsSyncResponse()
                .accounts(List.of())
                .added(List.of())
                .modified(List.of())
                .nextCursor(cursor)
                .hasMore(hasMore);
    }

    private void stubExchangeAndEmptySync() {
        when(plaidClient.exchangePublicToken(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(new ItemPublicTokenExchangeResponse().itemId("item-1").accessToken("access-token"));
        Connection savedConnection = connection(true);
        when(connectionService.create(any(Connection.class))).thenReturn(savedConnection);
        when(connectionService.readByIdForUpdate(CONNECTION_ID)).thenReturn(savedConnection);
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class))).thenReturn(syncResponse("cursor-1", false));
    }

    private ExchangeTokenRequestDto duplicateCheckRequest(String mask, String subtype) {
        return new ExchangeTokenRequestDto(
                "public-token",
                List.of("account-1"),
                List.of(new ConnectionAccountMetadataDto("account-1", "Checking", mask, subtype)),
                "ins-1",
                "Test Bank");
    }

    private AccountBase account(String accountId) {
        return new AccountBase()
                .accountId(accountId)
                .name("Checking")
                .type(AccountType.DEPOSITORY)
                .subtype(AccountSubtype.CHECKING)
                .mask("0000")
                .balances(new AccountBalance().current(100.00).available(90.00).isoCurrencyCode("USD"));
    }

    private Transaction transaction(String transactionId, String accountId) {
        return new Transaction()
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(15.75)
                .isoCurrencyCode("USD")
                .name("Coffee Shop")
                .transactionType(Transaction.TransactionTypeEnum.PLACE)
                .date(LocalDate.of(2026, 5, 7))
                .pending(false)
                .paymentChannel(Transaction.PaymentChannelEnum.IN_STORE)
                .personalFinanceCategory(new PersonalFinanceCategory().detailed("FOOD_AND_DRINK_COFFEE"));
    }

    private AccountResponseDto accountResponse(String mask, String subtype) {
        return new AccountResponseDto(ACCOUNT_ID, "Test Bank", "Checking", "depository", subtype, mask,
                BigDecimal.valueOf(100), BigDecimal.valueOf(90), "USD", Instant.EPOCH, Instant.EPOCH);
    }

    private AccountUpsertResponseDto accountUpsertResponse(UUID accountId) {
        return new AccountUpsertResponseDto(accountId, USER_ID, CONNECTION_ID, "Test Bank", "Checking", "depository",
                "checking", "0000", BigDecimal.valueOf(100), BigDecimal.valueOf(90), "USD", false,
                Instant.EPOCH, Instant.EPOCH);
    }

    private AccountUpsertRequestDto accountRequest() {
        return new AccountUpsertRequestDto(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Test Bank", "Checking", "depository",
                "checking", "0000", BigDecimal.valueOf(100), BigDecimal.valueOf(90), "USD");
    }

    private TransactionUpsertRequestDto transactionRequest() {
        return new TransactionUpsertRequestDto(TRANSACTION_ID, USER_ID, ACCOUNT_ID, BigDecimal.valueOf(15.75), "USD",
                "Coffee Shop", "place", LocalDate.of(2026, 5, 7), false, "in store", "FOOD_AND_DRINK_COFFEE", true);
    }
}
