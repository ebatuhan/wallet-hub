package com.batu.transaction_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.transaction_service.entity.OutboxEvent;
import com.batu.transaction_service.messaging.outbox.ScheduledOutboxRelay;
import com.batu.transaction_service.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class ScheduledOutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishPending_whenOutboxEventExists_shouldPublishAndMarkEventPublished() {
        OutboxEvent outboxEvent = recordedEvent(UUID.fromString("e5000000-0000-0000-0000-000000000001"));
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(outboxEvent));

        new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending();

        verify(rabbitTemplate).send(
                eq(MessagingTopology.EXCHANGE_NAME),
                eq(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY),
                argThat(message -> isJsonMessage(message) && new String(message.getBody()).contains("transaction.recorded.v1")));
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPending_whenNoOutboxEventsExist_shouldNotPublishMessages() {
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of());

        new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Message.class));
    }

    @Test
    void publishPending_whenMultipleOutboxEventsExist_shouldPublishAllAndMarkAllPublished() {
        OutboxEvent recorded = recordedEvent(UUID.fromString("e5000000-0000-0000-0000-000000000001"));
        OutboxEvent removed = new OutboxEvent(
                MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY,
                EventTypes.TRANSACTION_REMOVED,
                "transaction",
                UUID.fromString("e5000000-0000-0000-0000-000000000002"),
                "{\"eventType\":\"TransactionRemoved\"}");
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(recorded, removed));

        new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending();

        verify(rabbitTemplate).send(eq(MessagingTopology.EXCHANGE_NAME), eq(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY), org.mockito.ArgumentMatchers.any(Message.class));
        verify(rabbitTemplate).send(eq(MessagingTopology.EXCHANGE_NAME), eq(MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY), org.mockito.ArgumentMatchers.any(Message.class));
        assertThat(recorded.getPublishedAt()).isNotNull();
        assertThat(removed.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPending_whenRabbitPublishFails_shouldLeaveEventUnpublished() {
        OutboxEvent outboxEvent = recordedEvent(UUID.fromString("e5000000-0000-0000-0000-000000000003"));
        RuntimeException failure = new RuntimeException("rabbit unavailable");
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(outboxEvent));
        doThrow(failure).when(rabbitTemplate).send(
                eq(MessagingTopology.EXCHANGE_NAME),
                eq(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY),
                org.mockito.ArgumentMatchers.any(Message.class));

        assertThatThrownBy(() -> new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending())
                .isSameAs(failure);
        assertThat(outboxEvent.getPublishedAt()).isNull();
    }

    private OutboxEvent recordedEvent(UUID transactionId) {
        return new OutboxEvent(
                MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                EventTypes.TRANSACTION_RECORDED,
                "transaction",
                transactionId,
                "{\"eventType\":\"transaction.recorded.v1\"}");
    }

    private boolean isJsonMessage(Message message) {
        return MessageProperties.CONTENT_TYPE_JSON.equals(message.getMessageProperties().getContentType());
    }
}
