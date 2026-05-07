package com.batu.insights_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.insights_service.config.InboxJpaConfiguration;
import com.batu.insights_service.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@SpringBootTest(classes = InsightsInboxIT.TestApplication.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "management.otlp.metrics.export.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InsightsInboxIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private InsightsInbox insightsInbox;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @DynamicPropertySource
    static void inboxProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("wallet-hub.inbox.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("wallet-hub.inbox.datasource.username", POSTGRES::getUsername);
        registry.add("wallet-hub.inbox.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanInbox() {
        inboxEventRepository.deleteAll();
    }

    @Test
    void process_whenSameEventIsProcessedSequentially_shouldRunHandlerOnlyOnce() {
        AtomicInteger handlerCalls = new AtomicInteger();
        UUID eventId = UUID.fromString("9e000000-0000-0000-0000-000000000001");
        BaseEvent<String> event = event(eventId);

        insightsInbox.process(event, handlerCalls::incrementAndGet);
        insightsInbox.process(event, handlerCalls::incrementAndGet);

        assertThat(handlerCalls).hasValue(1);
        assertThat(inboxEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void process_whenHandlerFails_shouldRollbackInboxClaimSoEventCanBeRetried() {
        UUID eventId = UUID.fromString("9e000000-0000-0000-0000-000000000003");
        BaseEvent<String> event = event(eventId);

        assertThatThrownBy(() -> insightsInbox.process(event, () -> {
            throw new IllegalStateException("handler failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(inboxEventRepository.existsById(eventId)).isFalse();

        AtomicInteger handlerCalls = new AtomicInteger();
        insightsInbox.process(event, handlerCalls::incrementAndGet);

        assertThat(handlerCalls).hasValue(1);
        assertThat(inboxEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void process_whenSameEventIsDeliveredConcurrently_shouldRunHandlerOnlyOnce() throws Exception {
        AtomicInteger handlerCalls = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        UUID eventId = UUID.fromString("9e000000-0000-0000-0000-000000000004");
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
            insightsInbox.process(event, handlerCalls::incrementAndGet);
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
                UUID.fromString("9e000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-05-06T10:15:30Z"),
                "payload");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
    @Import({ InsightsInbox.class, InboxJpaConfiguration.class })
    static class TestApplication {
    }
}
