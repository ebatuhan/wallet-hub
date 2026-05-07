package com.batu.transaction_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.transaction_service.TransactionServiceApplication;
import com.batu.transaction_service.entity.OutboxEvent;
import com.batu.transaction_service.messaging.outbox.ScheduledOutboxRelay;
import com.batu.transaction_service.repository.OutboxEventRepository;

@SpringBootTest(
        classes = { TransactionServiceApplication.class, ScheduledOutboxRelayIT.TestcontainersConfiguration.class },
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.show_sql=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "spring.rabbitmq.connection-timeout=1s",
                "wallet-hub.outbox.relay-delay-ms=600000",
                "spring.task.scheduling.enabled=false",
                "spring.task.scheduling.shutdown.await-termination=false",
                "spring.lifecycle.timeout-per-shutdown-phase=1s",
                "management.otlp.metrics.export.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@TestMethodOrder(OrderAnnotation.class)
class ScheduledOutboxRelayIT {

    private static final UUID TRANSACTION_ID = UUID.fromString("d3000000-0000-0000-0000-000000000001");
    private static final String RECORDED_QUEUE = "transaction-service-test.transaction-recorded";
    private static final String REMOVED_QUEUE = "transaction-service-test.transaction-removed";

    @Autowired
    private ScheduledOutboxRelay scheduledOutboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DelayingRabbitTemplate delayingRabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitMQContainer rabbitmqContainer;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        amqpAdmin.deleteQueue(RECORDED_QUEUE);
        amqpAdmin.deleteQueue(REMOVED_QUEUE);
        declareExchange();
        delayingRabbitTemplate.clearDelay();
    }

    @AfterEach
    void tearDown() {
        delayingRabbitTemplate.clearDelay();
    }

    @Test
    @Order(1)
    void publishPending_whenTransactionRecordedEventExists_shouldPublishToRecordedRoutingKeyAndMarkSent() {
        declareQueue(RECORDED_QUEUE, MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY);
        OutboxEvent outboxEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                EventTypes.TRANSACTION_RECORDED,
                "transaction",
                TRANSACTION_ID,
                "{\"eventType\":\"%s\",\"aggregateId\":\"%s\"}".formatted(EventTypes.TRANSACTION_RECORDED, TRANSACTION_ID)));

        scheduledOutboxRelay.publishPending();

        Message message = rabbitTemplate.receive(RECORDED_QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody())).contains(EventTypes.TRANSACTION_RECORDED, TRANSACTION_ID.toString());
        assertThat(outboxEventRepository.findById(outboxEvent.getOutboxEventId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    @Order(2)
    void publishPending_whenTransactionRemovedEventExists_shouldPublishToRemovedRoutingKeyAndMarkSent() {
        declareQueue(REMOVED_QUEUE, MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY);
        OutboxEvent outboxEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY,
                EventTypes.TRANSACTION_REMOVED,
                "transaction",
                TRANSACTION_ID,
                "{\"eventType\":\"%s\",\"aggregateId\":\"%s\"}".formatted(EventTypes.TRANSACTION_REMOVED, TRANSACTION_ID)));

        scheduledOutboxRelay.publishPending();

        Message message = rabbitTemplate.receive(REMOVED_QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody())).contains(EventTypes.TRANSACTION_REMOVED, TRANSACTION_ID.toString());
        assertThat(outboxEventRepository.findById(outboxEvent.getOutboxEventId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    @Order(3)
    void publishPending_whenRelaysRace_shouldPublishPendingEventOnlyOnce() throws Exception {
        declareQueue(RECORDED_QUEUE, MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY);
        OutboxEvent outboxEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                EventTypes.TRANSACTION_RECORDED,
                "transaction",
                TRANSACTION_ID,
                "{\"eventType\":\"%s\",\"aggregateId\":\"%s\"}".formatted(EventTypes.TRANSACTION_RECORDED, TRANSACTION_ID)));
        delayingRabbitTemplate.delayFirstSend();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstRelay = executor.submit(() -> scheduledOutboxRelay.publishPending());
            assertThat(delayingRabbitTemplate.awaitBlocked()).isTrue();

            Future<?> racingRelay = executor.submit(() -> scheduledOutboxRelay.publishPending());
            racingRelay.get(5, TimeUnit.SECONDS);
            delayingRabbitTemplate.releaseBlockedSend();
            firstRelay.get(5, TimeUnit.SECONDS);
        } finally {
            delayingRabbitTemplate.releaseBlockedSend();
            executor.shutdownNow();
        }

        List<Message> messages = receiveAvailableMessages(RECORDED_QUEUE);
        assertThat(messages).hasSize(1);
        assertThat(new String(messages.getFirst().getBody())).contains(EventTypes.TRANSACTION_RECORDED, TRANSACTION_ID.toString());
        assertThat(outboxEventRepository.findById(outboxEvent.getOutboxEventId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    @Order(4)
    void publishPending_whenRabbitPublishFails_shouldLeaveEventRetryable() {
        OutboxEvent outboxEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                EventTypes.TRANSACTION_RECORDED,
                "transaction",
                TRANSACTION_ID,
                "{\"eventType\":\"TransactionRecorded\"}"));
        rabbitmqContainer.stop();

        assertThatThrownBy(() -> scheduledOutboxRelay.publishPending()).isInstanceOf(RuntimeException.class);

        assertThat(outboxEventRepository.findById(outboxEvent.getOutboxEventId()).orElseThrow().getPublishedAt()).isNull();
        outboxEventRepository.deleteById(outboxEvent.getOutboxEventId());
    }

    private void declareExchange() {
        amqpAdmin.declareExchange(new TopicExchange(MessagingTopology.EXCHANGE_NAME, true, false));
    }

    private void declareQueue(String queueName, String routingKey) {
        Queue queue = new Queue(queueName, false, false, false);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue)
                .to(new TopicExchange(MessagingTopology.EXCHANGE_NAME, true, false))
                .with(routingKey));
    }

    private List<Message> receiveAvailableMessages(String queueName) {
        List<Message> messages = new java.util.ArrayList<>();
        Message firstMessage = rabbitTemplate.receive(queueName, 5_000);
        if (firstMessage != null) {
            messages.add(firstMessage);
        }
        Message nextMessage;
        while ((nextMessage = rabbitTemplate.receive(queueName, 100)) != null) {
            messages.add(nextMessage);
        }
        return messages;
    }

    static class DelayingRabbitTemplate extends RabbitTemplate {
        private final AtomicInteger delayRequests = new AtomicInteger();
        private volatile CountDownLatch blockedSend;
        private volatile CountDownLatch releaseSend;

        DelayingRabbitTemplate(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        void delayFirstSend() {
            delayRequests.set(1);
            blockedSend = new CountDownLatch(1);
            releaseSend = new CountDownLatch(1);
        }

        boolean awaitBlocked() throws InterruptedException {
            CountDownLatch latch = blockedSend;
            return latch != null && latch.await(5, TimeUnit.SECONDS);
        }

        void releaseBlockedSend() {
            CountDownLatch latch = releaseSend;
            if (latch != null) {
                latch.countDown();
            }
        }

        void clearDelay() {
            delayRequests.set(0);
            releaseBlockedSend();
            blockedSend = null;
            releaseSend = null;
        }

        @Override
        public void send(String exchange, String routingKey, Message message) {
            if (delayRequests.getAndUpdate(count -> count > 0 ? count - 1 : count) > 0) {
                blockedSend.countDown();
                try {
                    assertThat(releaseSend.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            super.send(exchange, routingKey, message);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @Primary
        DelayingRabbitTemplate delayingRabbitTemplate(ConnectionFactory connectionFactory) {
            return new DelayingRabbitTemplate(connectionFactory);
        }

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitmqContainer() {
            return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-alpine"));
        }
    }
}
