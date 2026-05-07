package com.batu.insights_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.batu.insights_service.config.InboxJpaConfiguration;
import com.batu.insights_service.config.RabbitMQConfiguration;
import com.batu.insights_service.repository.InboxEventRepository;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;
import com.batu.shared.messaging.event.TransactionRecorded;

@SpringBootTest(classes = InsightsRabbitMQIT.TestApplication.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "management.otlp.metrics.export.enabled=false",
        "spring.rabbitmq.listener.simple.concurrency=2",
        "spring.rabbitmq.listener.simple.max-concurrency=2"
})
class InsightsRabbitMQIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final UUID EVENT_ID = UUID.fromString("9c000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("9c000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("9c000000-0000-0000-0000-000000000003");
    private static final UUID CONNECTION_ID = UUID.fromString("9c000000-0000-0000-0000-000000000004");
    private static final UUID TRANSACTION_ID = UUID.fromString("9c000000-0000-0000-0000-000000000005");
    private static final UUID CATEGORY_ID = UUID.fromString("9c000000-0000-0000-0000-000000000006");
    private static final Instant OCCURRED_AT = Instant.parse("2026-05-07T09:15:30Z");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @MockitoBean
    private TransactionInsightsService transactionInsightsService;

    @MockitoBean
    private AccountInsightsService accountInsightsService;

    @DynamicPropertySource
    static void inboxProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("wallet-hub.inbox.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("wallet-hub.inbox.datasource.username", POSTGRES::getUsername);
        registry.add("wallet-hub.inbox.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void transactionRecordedMessage_whenPublishedToRabbit_shouldReachListenerAndProjectTransactionRow() throws Exception {
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY,
                transactionRecordedEvent());

        verify(transactionInsightsService, timeout(10_000)).save(argThat(row ->
                row.transactionId().equals(TRANSACTION_ID)
                        && row.userId().equals(USER_ID)
                        && row.accountId().equals(ACCOUNT_ID)
                        && row.primaryCategoryId().equals(CATEGORY_ID)
                        && row.amount().compareTo(new BigDecimal("-42.50")) == 0
                        && row.isOutflow()
                        && row.isActive()
                        && row.isoCurrencyCode().equals("USD")
                        && row.date().equals(LocalDate.of(2026, 5, 7))
                        && row.updatedAt().equals(OCCURRED_AT)));
        assertEventually(() -> inboxEventRepository.existsById(EVENT_ID));
    }

    @Test
    void accountRecordedMessage_whenPublishedToRabbit_shouldReachListenerAndProjectBalancePoint() throws Exception {
        UUID eventId = UUID.fromString("9c000000-0000-0000-0000-000000000201");
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY,
                accountRecordedEvent(eventId));

        verify(accountInsightsService, timeout(10_000)).save(argThat(row ->
                row.accountId().equals(ACCOUNT_ID)
                        && row.userId().equals(USER_ID)
                        && row.balance().compareTo(new BigDecimal("1250.75")) == 0
                        && row.isoCurrencyCode().equals("USD")
                        && row.date().equals(LocalDate.of(2026, 5, 7))));
        assertEventually(() -> inboxEventRepository.existsById(eventId));
    }

    @Test
    void accountRemovedMessage_whenPublishedToRabbit_shouldReachListenerAndRemoveAccountProjections() throws Exception {
        UUID eventId = UUID.fromString("9c000000-0000-0000-0000-000000000202");
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY,
                accountRemovedEvent(eventId));

        verify(accountInsightsService, timeout(10_000)).remove(argThat(event ->
                event.getAccountId().equals(ACCOUNT_ID)
                        && event.getUserId().equals(USER_ID)
                        && event.getConnectionId().equals(CONNECTION_ID)));
        verify(transactionInsightsService, timeout(10_000)).removeAccountTransactions(ACCOUNT_ID, USER_ID);
        assertEventually(() -> inboxEventRepository.existsById(eventId));
    }

    @Test
    void duplicateTransactionRecordedMessage_whenPublishedTwice_shouldProjectOnceAndRecordOneInboxRow() throws Exception {
        BaseEvent<TransactionRecorded> event = transactionRecordedEvent(
                UUID.fromString("9c000000-0000-0000-0000-000000000101"));

        rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE_NAME, MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE_NAME, MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY, event);

        verify(transactionInsightsService, timeout(10_000).times(1)).save(any());
        assertEventually(() -> inboxEventRepository.existsById(event.getEventId()));
    }

    @Test
    void duplicateTransactionRecordedMessage_whenDeliveredConcurrently_shouldProjectOnceAndRecordOneInboxRow()
            throws Exception {
        BaseEvent<TransactionRecorded> event = transactionRecordedEvent(
                UUID.fromString("9c000000-0000-0000-0000-000000000102"));
        CountDownLatch handlerStarted = new CountDownLatch(1);
        CountDownLatch releaseHandler = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation -> {
            handlerStarted.countDown();
            assertThat(releaseHandler.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(transactionInsightsService).save(any());

        rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE_NAME, MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE_NAME, MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY, event);

        assertThat(handlerStarted.await(10, TimeUnit.SECONDS)).isTrue();
        releaseHandler.countDown();

        verify(transactionInsightsService, timeout(10_000).times(1)).save(any());
        assertEventually(() -> inboxEventRepository.existsById(event.getEventId()));
    }

    private BaseEvent<TransactionRecorded> transactionRecordedEvent() {
        return transactionRecordedEvent(EVENT_ID);
    }

    private BaseEvent<TransactionRecorded> transactionRecordedEvent(UUID eventId) {
        return new BaseEvent<>(
                eventId,
                EventTypes.TRANSACTION_RECORDED,
                "transaction-service",
                eventId,
                null,
                "transaction",
                TRANSACTION_ID,
                OCCURRED_AT,
                new TransactionRecorded(
                        TRANSACTION_ID,
                        USER_ID,
                        ACCOUNT_ID,
                        new BigDecimal("-42.50"),
                        "USD",
                        "Coffee",
                        "PLACE",
                        LocalDate.of(2026, 5, 7),
                        false,
                        "in store",
                        CATEGORY_ID,
                        "FOOD_AND_DRINK",
                        true));
    }

    private BaseEvent<AccountRecorded> accountRecordedEvent(UUID eventId) {
        return new BaseEvent<>(
                eventId,
                EventTypes.ACCOUNT_RECORDED,
                "account-service",
                eventId,
                null,
                "account",
                ACCOUNT_ID,
                OCCURRED_AT,
                new AccountRecorded(
                        ACCOUNT_ID,
                        USER_ID,
                        CONNECTION_ID,
                        "Test Bank",
                        "Checking",
                        "depository",
                        "checking",
                        "1234",
                        new BigDecimal("1250.75"),
                        new BigDecimal("1200.00"),
                        "USD",
                        true));
    }

    private BaseEvent<AccountRemoved> accountRemovedEvent(UUID eventId) {
        return new BaseEvent<>(
                eventId,
                EventTypes.ACCOUNT_REMOVED,
                "account-service",
                eventId,
                null,
                "account",
                ACCOUNT_ID,
                OCCURRED_AT,
                new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID));
    }

    private void assertEventually(BooleanSupplier assertion) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (assertion.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(assertion.getAsBoolean()).isTrue();
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean() throws InterruptedException;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
    @EnableRabbit
    @Import({ RabbitMQConfiguration.class, TransactionEventListener.class, AccountEventListener.class,
            InsightsInbox.class, InboxJpaConfiguration.class, TestcontainersConfiguration.class })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitmqContainer() {
            return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-alpine"));
        }

    }
}
