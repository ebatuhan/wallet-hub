package com.batu.plaid_adapter_service.integration;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.plaid_adapter_service.client.AccountClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.service.impl.ConnectionServiceImpl;
import com.batu.plaid_adapter_service.service.impl.PlaidIntegrationServiceImpl;
import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.request.ConnectionAccountMetadataDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountSubtype;
import com.plaid.client.model.AccountType;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "plaid.webhook.url=https://wallet.example/webhook"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PlaidIntegrationServiceImpl.class,
        ConnectionServiceImpl.class,
        ConnectionMapper.class,
        PlaidRequestMapper.class,
        DeterministicIdGenerator.class,
        PlaidIntegrationServiceIT.PostgreSqlTestcontainersConfiguration.class
})
class PlaidIntegrationServiceIT {

    private static final UUID USER_ID = UUID.fromString("84000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("84000000-0000-0000-0000-000000000002");

    @Autowired
    private PlaidIntegrationService plaidIntegrationService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DeterministicIdGenerator deterministicIdGenerator;

    @MockitoBean
    private PlaidClientWrapper plaidClient;

    @MockitoBean
    private AccountClient accountClient;

    @MockitoBean
    private TransactionClient transactionClient;

    @Test
    void exchangeLinkToken_whenPlaidAndSyncSucceed_shouldPersistConnectionAndCompleteSyncCursor() {
        when(plaidClient.exchangePublicToken(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(new ItemPublicTokenExchangeResponse().itemId("item-exchange").accessToken("access-token"));
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class))).thenReturn(syncResponse("cursor-1", false));

        var response = plaidIntegrationService.exchangeLinkToken(
                new ExchangeTokenRequestDto("public-token", List.of(), List.of(), "ins-1", "Test Bank"),
                USER_ID);
        entityManager.flush();
        entityManager.clear();

        Connection persisted = connectionRepository.findById(response.getConnectionId()).orElseThrow();
        assertThat(persisted.getUserId()).isEqualTo(USER_ID);
        assertThat(persisted.getExternalId()).isEqualTo("item-exchange");
        assertThat(persisted.getInstitutionId()).isEqualTo("ins-1");
        assertThat(persisted.getLastCursor()).isEqualTo("cursor-1");
        assertThat(persisted.getLastSyncedAt()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void exchangeLinkToken_whenSyncFails_shouldRollbackCreatedConnection() {
        when(plaidClient.exchangePublicToken(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(new ItemPublicTokenExchangeResponse().itemId("item-rollback").accessToken("access-token"));
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenThrow(new PlaidRetryableException("Plaid unavailable", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> plaidIntegrationService.exchangeLinkToken(
                new ExchangeTokenRequestDto("public-token", List.of(), List.of(), "ins-1", "Test Bank"),
                USER_ID))
                .isInstanceOf(PlaidRetryableException.class);
        entityManager.clear();

        assertThat(connectionRepository.findByExternalId("item-rollback")).isEmpty();
    }

    @Test
    void exchangeLinkToken_whenIncomingAccountDuplicatesPersistedConnection_shouldThrowConflictAndNotCallPlaid() {
        Connection existing = saveConnection(USER_ID, "item-existing", true, "ins-1", "Test Bank");
        when(accountClient.findAccountsByConnectionId(existing.getConnectionId())).thenReturn(List.of(accountResponse("0000", "checking")));

        ExchangeTokenRequestDto request = new ExchangeTokenRequestDto(
                "public-token",
                List.of("account-1"),
                List.of(new ConnectionAccountMetadataDto("account-1", "Checking", " 0000 ", " CHECKING ")),
                "ins-1",
                "Test Bank");

        assertThatThrownBy(() -> plaidIntegrationService.exchangeLinkToken(request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(plaidClient, never()).exchangePublicToken(any());
    }

    @Test
    void syncConnection_whenActiveConnectionHasAccountsAddedAndModifiedTransactions_shouldUpsertDownstreamAndPersistCursor() {
        Connection connection = saveConnection(USER_ID, "item-sync", true, "ins-1", "Test Bank");
        AccountBase plaidAccount = plaidAccount("plaid-account-1");
        Transaction added = plaidTransaction("plaid-added", "plaid-account-1", "Added Coffee");
        Transaction modified = plaidTransaction("plaid-modified", "plaid-account-1", "Modified Coffee");
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(new TransactionsSyncResponse()
                        .accounts(List.of(plaidAccount))
                        .added(List.of(added))
                        .modified(List.of(modified))
                        .nextCursor("cursor-2")
                        .hasMore(false));

        plaidIntegrationService.syncConnection(connection.getConnectionId());
        entityManager.flush();
        entityManager.clear();

        UUID accountId = deterministicIdGenerator.accountId(USER_ID, "plaid-account-1");
        UUID addedId = deterministicIdGenerator.transactionId(USER_ID, "plaid-account-1", "plaid-added");
        UUID modifiedId = deterministicIdGenerator.transactionId(USER_ID, "plaid-account-1", "plaid-modified");
        verify(accountClient).upsertAccount(any(AccountUpsertRequestDto.class));
        verify(transactionClient, times(2)).upsertTransaction(any(TransactionUpsertRequestDto.class));
        Connection persisted = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertThat(persisted.getLastCursor()).isEqualTo("cursor-2");
        assertThat(accountId).isNotNull();
        assertThat(addedId).isNotEqualTo(modifiedId);
    }

    @Test
    void syncConnection_whenPlaidHasMultiplePages_shouldPersistLastCursor() {
        Connection connection = saveConnection(USER_ID, "item-pages", true, "ins-1", "Test Bank");
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(syncResponse("cursor-page-1", true))
                .thenReturn(syncResponse("cursor-page-2", false));

        plaidIntegrationService.syncConnection(connection.getConnectionId());
        entityManager.flush();
        entityManager.clear();

        assertThat(connectionRepository.findById(connection.getConnectionId()).orElseThrow().getLastCursor())
                .isEqualTo("cursor-page-2");
    }

    @Test
    void syncConnection_whenConnectionInactive_shouldNotCallPlaidOrChangeCursor() {
        Connection connection = saveConnection(USER_ID, "item-inactive", false, "ins-1", "Test Bank");
        connection.setLastCursor("old-cursor");
        connectionRepository.saveAndFlush(connection);
        entityManager.clear();

        plaidIntegrationService.syncConnection(connection.getConnectionId());
        entityManager.flush();
        entityManager.clear();

        verify(plaidClient, never()).syncTransactions(any());
        assertThat(connectionRepository.findById(connection.getConnectionId()).orElseThrow().getLastCursor())
                .isEqualTo("old-cursor");
    }

    @Test
    void syncConnection_whenDownstreamAccountUpsertFails_shouldPropagateAndLeaveCursorUnchanged() {
        Connection connection = saveConnection(USER_ID, "item-downstream-failure", true, "ins-1", "Test Bank");
        when(plaidClient.syncTransactions(any(TransactionsSyncRequest.class)))
                .thenReturn(new TransactionsSyncResponse()
                        .accounts(List.of(plaidAccount("plaid-account-1")))
                        .added(List.of())
                        .modified(List.of())
                        .nextCursor("cursor-2")
                        .hasMore(false));
        when(accountClient.upsertAccount(any(AccountUpsertRequestDto.class))).thenThrow(new IllegalStateException("account down"));

        assertThatThrownBy(() -> plaidIntegrationService.syncConnection(connection.getConnectionId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("account down");
        entityManager.clear();

        assertThat(connectionRepository.findById(connection.getConnectionId()).orElseThrow().getLastCursor()).isNull();
    }

    @Test
    void removeConnection_whenActiveConnectionExists_shouldRemovePlaidItemDeactivateDownstreamAndPersistInactive() {
        Connection connection = saveConnection(USER_ID, "item-remove", true, "ins-1", "Test Bank");
        UUID accountId = UUID.fromString("84000000-0000-0000-0000-000000000099");
        when(accountClient.deactivateAccountsByConnection(connection.getConnectionId()))
                .thenReturn(List.of(accountUpsertResponse(accountId, connection.getConnectionId())));

        plaidIntegrationService.removeConnection(connection.getConnectionId(), "USER_REQUESTED_REMOVAL");
        entityManager.flush();
        entityManager.clear();

        verify(plaidClient).removeItem(any());
        verify(transactionClient).deactivateTransactionsByAccount(accountId);
        Connection persisted = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertThat(persisted.isActive()).isFalse();
        assertThat(persisted.getErrorCode()).isEqualTo("USER_REQUESTED_REMOVAL");
    }

    @Test
    void removeConnection_whenConnectionInactive_shouldNotCallPlaidOrDownstream() {
        Connection connection = saveConnection(USER_ID, "item-remove-inactive", false, "ins-1", "Test Bank");

        plaidIntegrationService.removeConnection(connection.getConnectionId(), "USER_REQUESTED_REMOVAL");

        verify(plaidClient, never()).removeItem(any());
        verify(accountClient, never()).deactivateAccountsByConnection(any());
        verify(transactionClient, never()).deactivateTransactionsByAccount(any());
    }

    private Connection saveConnection(UUID userId, String externalId, boolean active, String institutionId, String institutionName) {
        Connection connection = new Connection(userId, externalId, "access-token-" + externalId, institutionId,
                institutionName);
        connection.setActive(active);
        Connection saved = connectionRepository.saveAndFlush(connection);
        entityManager.clear();
        return saved;
    }

    private TransactionsSyncResponse syncResponse(String cursor, boolean hasMore) {
        return new TransactionsSyncResponse()
                .accounts(List.of())
                .added(List.of())
                .modified(List.of())
                .nextCursor(cursor)
                .hasMore(hasMore);
    }

    private AccountBase plaidAccount(String accountId) {
        return new AccountBase()
                .accountId(accountId)
                .name("Checking")
                .type(AccountType.DEPOSITORY)
                .subtype(AccountSubtype.CHECKING)
                .mask("0000")
                .balances(new AccountBalance().current(100.00).available(90.00).isoCurrencyCode("USD"));
    }

    private Transaction plaidTransaction(String transactionId, String accountId, String name) {
        return new Transaction()
                .transactionId(transactionId)
                .accountId(accountId)
                .amount(15.75)
                .isoCurrencyCode("USD")
                .name(name)
                .transactionType(Transaction.TransactionTypeEnum.PLACE)
                .date(LocalDate.of(2026, 5, 7))
                .pending(false)
                .paymentChannel(Transaction.PaymentChannelEnum.IN_STORE)
                .personalFinanceCategory(new PersonalFinanceCategory().detailed("FOOD_AND_DRINK_COFFEE"));
    }

    private AccountResponseDto accountResponse(String mask, String subtype) {
        return new AccountResponseDto(UUID.randomUUID(), "Test Bank", "Checking", "depository", subtype, mask,
                BigDecimal.valueOf(100), BigDecimal.valueOf(90), "USD", Instant.EPOCH, Instant.EPOCH);
    }

    private AccountUpsertResponseDto accountUpsertResponse(UUID accountId, UUID connectionId) {
        return new AccountUpsertResponseDto(accountId, OTHER_USER_ID, connectionId, "Test Bank", "Checking", "depository",
                "checking", "0000", BigDecimal.valueOf(100), BigDecimal.valueOf(90), "USD", false,
                Instant.EPOCH, Instant.EPOCH);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
