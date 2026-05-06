package com.batu.budgeting.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realms/wallet-hub",
        "management.otlp.metrics.export.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(TransactionRecordedRabbitIT.TestcontainersConfiguration.class)
class TransactionRecordedRabbitIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @MockitoBean
    private BudgetService budgetService;

    @Test
    void transactionRecordedMessage_whenPublishedToRabbit_shouldReachListenerAndRecordInboxEvent()
            throws InterruptedException {
        UUID eventId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        UUID transactionId = UUID.fromString("90000000-0000-0000-0000-000000000002");

        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY,
                event(eventId, transactionId));

        verify(budgetService, timeout(10_000).times(1)).applyTransaction(argThat(transaction ->
                transaction.getTransactionId().equals(transactionId)
                        && transaction.getAmount().compareTo(new BigDecimal("-25.00")) == 0));
        assertEventually(() -> inboxEventRepository.existsById(eventId));
    }

    private void assertEventually(BooleanSupplier assertion) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000);
        while (System.nanoTime() < deadline) {
            if (assertion.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(assertion.getAsBoolean()).isTrue();
    }

    private BaseEvent<TransactionRecorded> event(UUID eventId, UUID transactionId) {
        return new BaseEvent<>(
                eventId,
                EventTypes.TRANSACTION_RECORDED,
                "transaction-service",
                eventId,
                null,
                "transaction",
                transactionId,
                Instant.parse("2026-05-06T10:15:30Z"),
                transaction(transactionId));
    }

    private TransactionRecorded transaction(UUID transactionId) {
        return new TransactionRecorded(
                transactionId,
                UUID.fromString("90000000-0000-0000-0000-000000000003"),
                UUID.fromString("90000000-0000-0000-0000-000000000004"),
                new BigDecimal("-25.00"),
                "USD",
                "Coffee",
                "PLACE",
                LocalDate.of(2026, 5, 6),
                false,
                "in store",
                UUID.fromString("90000000-0000-0000-0000-000000000005"),
                "FOOD_AND_DRINK",
                true);
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitmqContainer() {
            return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-alpine"));
        }
    }
}
