package com.batu.budgeting.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ BudgetingInbox.class, BudgetingInboxIT.PostgreSqlTestcontainersConfiguration.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BudgetingInboxIT {

    @Autowired
    private BudgetingInbox budgetingInbox;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Test
    void process_whenSameEventIsProcessedSequentially_shouldRunHandlerOnlyOnce() {
        AtomicInteger handlerCalls = new AtomicInteger();
        UUID eventId = UUID.fromString("80000000-0000-0000-0000-000000000001");
        BaseEvent<String> event = event(eventId);

        budgetingInbox.process(event, handlerCalls::incrementAndGet);
        budgetingInbox.process(event, handlerCalls::incrementAndGet);

        assertThat(handlerCalls).hasValue(1);
        assertThat(inboxEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void process_whenHandlerFails_shouldRollbackInboxClaimSoEventCanBeRetried() {
        UUID eventId = UUID.fromString("80000000-0000-0000-0000-000000000003");
        BaseEvent<String> event = event(eventId);

        assertThatThrownBy(() -> budgetingInbox.process(event, () -> {
            throw new IllegalStateException("handler failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(inboxEventRepository.existsById(eventId)).isFalse();

        AtomicInteger handlerCalls = new AtomicInteger();
        budgetingInbox.process(event, handlerCalls::incrementAndGet);

        assertThat(handlerCalls).hasValue(1);
        assertThat(inboxEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void process_whenSameEventIsDeliveredConcurrently_shouldRunHandlerOnlyOnce() throws Exception {
        AtomicInteger handlerCalls = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        UUID eventId = UUID.fromString("80000000-0000-0000-0000-000000000004");
        BaseEvent<String> event = event(eventId);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> processAfterStart(event, handlerCalls, ready, start));
            var second = executor.submit(() -> processAfterStart(event, handlerCalls, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        assertThat(handlerCalls).hasValue(1);
        assertThat(inboxEventRepository.existsById(eventId)).isTrue();
    }

    private void processAfterStart(BaseEvent<String> event, AtomicInteger handlerCalls, CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            budgetingInbox.process(event, handlerCalls::incrementAndGet);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private BaseEvent<String> event(UUID eventId) {
        return new BaseEvent<>(
                eventId,
                "TransactionRecorded",
                "transaction-service",
                eventId,
                null,
                "transaction",
                UUID.fromString("80000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-05-06T10:15:30Z"),
                "payload");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
