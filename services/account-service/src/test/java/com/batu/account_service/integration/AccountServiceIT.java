package com.batu.account_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.account_service.AccountServiceApplication;
import com.batu.account_service.entity.Account;
import com.batu.account_service.entity.OutboxEvent;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.OutboxEventRepository;
import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;

import jakarta.persistence.EntityManager;

@SpringBootTest(
        classes = { AccountServiceApplication.class, AccountServiceIT.PostgreSqlTestcontainersConfiguration.class },
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.show_sql=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "wallet-hub.outbox.relay-delay-ms=600000",
                "spring.task.scheduling.enabled=false",
                "management.otlp.metrics.export.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
class AccountServiceIT {

    private static final UUID ACCOUNT_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID SAVINGS_ACCOUNT_ID = UUID.fromString("d1000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000003");
    private static final UUID CONNECTION_ID = UUID.fromString("d1000000-0000-0000-0000-000000000004");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    void upsertAccount_whenValid_shouldPersistAccountAndCreateRecordedOutboxEventInSameTransaction() {
        AccountUpsertRequestDto request = upsertRequest(ACCOUNT_ID, "Checking", "100.00", "90.00");

        var response = accountService.upsertAccount(request);
        entityManager.clear();

        Account persistedAccount = accountRepository.findById(ACCOUNT_ID).orElseThrow();
        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(persistedAccount.getUserId()).isEqualTo(USER_ID);
        assertThat(persistedAccount.getConnectionId()).isEqualTo(CONNECTION_ID);
        assertThat(persistedAccount.getAccountName()).isEqualTo("Checking");
        assertThat(persistedAccount.getCurrentBalance()).isEqualByComparingTo("100.00");
        assertThat(persistedAccount.isActive()).isTrue();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).singleElement().satisfies(event -> {
            assertThat(event.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY);
            assertThat(event.getEventType()).isEqualTo(EventTypes.ACCOUNT_RECORDED);
            assertThat(event.getAggregateType()).isEqualTo("account");
            assertThat(event.getAggregateId()).isEqualTo(ACCOUNT_ID);
            assertThat(event.getPublishedAt()).isNull();
            assertThat(event.getPayload()).contains(EventTypes.ACCOUNT_RECORDED, ACCOUNT_ID.toString(), USER_ID.toString());
        });
    }

    @Test
    void upsertAccount_whenRepositoryFails_shouldRollbackAndCreateNoOutboxEvent() {
        AccountUpsertRequestDto invalidRequest = upsertRequest(ACCOUNT_ID, "Checking", "100.00", "90.00");
        invalidRequest.setInstitutionName(null);

        assertThatThrownBy(() -> accountService.upsertAccount(invalidRequest))
                .isInstanceOf(DataAccessException.class);
        entityManager.clear();

        assertThat(accountRepository.findById(ACCOUNT_ID)).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void deactivateAccountsByConnection_whenAccountsExist_shouldDeactivateAccountsAndCreateRemovedOutboxEvents() {
        Account checking = saveAccount(ACCOUNT_ID, "Checking");
        Account savings = saveAccount(SAVINGS_ACCOUNT_ID, "Savings");

        var response = accountService.deactivateAccountsByConnection(CONNECTION_ID);
        entityManager.clear();

        assertThat(response).hasSize(2).allSatisfy(account -> assertThat(account.isActive()).isFalse());
        assertThat(accountRepository.findById(checking.getAccountId()).orElseThrow().isActive()).isFalse();
        assertThat(accountRepository.findById(savings.getAccountId()).orElseThrow().isActive()).isFalse();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(2);
        assertThat(outboxEvents).allSatisfy(event -> {
            assertThat(event.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
            assertThat(event.getEventType()).isEqualTo(EventTypes.ACCOUNT_REMOVED);
            assertThat(event.getAggregateType()).isEqualTo("account");
            assertThat(event.getPublishedAt()).isNull();
            assertThat(event.getPayload()).contains(EventTypes.ACCOUNT_REMOVED, USER_ID.toString(), CONNECTION_ID.toString());
        });
        assertThat(outboxEvents).extracting(OutboxEvent::getAggregateId)
                .containsExactlyInAnyOrder(ACCOUNT_ID, SAVINGS_ACCOUNT_ID);
    }

    @Test
    void upsertAccount_whenSameAccountIsUpsertedConcurrently_shouldKeepSingleAccountAndRecordBothEvents() throws Exception {
        AccountUpsertRequestDto checkingRequest = upsertRequest(ACCOUNT_ID, "Checking", "100.00", "90.00");
        AccountUpsertRequestDto updatedRequest = upsertRequest(ACCOUNT_ID, "Updated Checking", "200.00", "180.00");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> upsertAfterStart(start, checkingRequest));
            Future<?> second = executor.submit(() -> upsertAfterStart(start, updatedRequest));

            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        entityManager.clear();

        List<Account> accounts = accountRepository.findAll();
        assertThat(accounts).singleElement().satisfies(account -> {
            assertThat(account.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(account.getAccountName()).isIn("Checking", "Updated Checking");
            assertThat(account.isActive()).isTrue();
        });
        assertThat(outboxEventRepository.findAll())
                .hasSize(2)
                .allSatisfy(event -> {
                    assertThat(event.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY);
                    assertThat(event.getAggregateId()).isEqualTo(ACCOUNT_ID);
                });
    }

    private Account saveAccount(UUID accountId, String accountName) {
        Account account = new Account(
                accountId,
                USER_ID,
                CONNECTION_ID,
                "Bank",
                accountName,
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                true);
        Account savedAccount = accountRepository.saveAndFlush(account);
        entityManager.clear();
        return savedAccount;
    }

    private AccountUpsertRequestDto upsertRequest(UUID accountId, String accountName, String currentBalance, String availableBalance) {
        return new AccountUpsertRequestDto(
                accountId,
                USER_ID,
                CONNECTION_ID,
                "Bank",
                accountName,
                "depository",
                "checking",
                "1234",
                new BigDecimal(currentBalance),
                new BigDecimal(availableBalance),
                "USD");
    }

    private void upsertAfterStart(CountDownLatch start, AccountUpsertRequestDto request) {
        try {
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            accountService.upsertAccount(request);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
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
