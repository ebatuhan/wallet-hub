package com.batu.account_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.batu.account_service.entity.OutboxEvent;
import com.batu.account_service.messaging.outbox.ScheduledOutboxRelay;
import com.batu.account_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;

@ExtendWith(MockitoExtension.class)
class ScheduledOutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishPending_whenOutboxEventExists_shouldPublishAndMarkEventPublished() {
        OutboxEvent outboxEvent = new OutboxEvent(
                MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY,
                EventTypes.ACCOUNT_RECORDED,
                "account",
                UUID.fromString("a2000000-0000-0000-0000-000000000001"),
                "{\"eventType\":\"account.recorded.v1\"}");
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(outboxEvent));

        new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending();

        verify(rabbitTemplate).send(
                eq(MessagingTopology.EXCHANGE_NAME),
                eq(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY),
                argThat(message -> isJsonMessage(message) && new String(message.getBody()).contains("account.recorded.v1")));
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
        OutboxEvent recorded = new OutboxEvent(
                MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY,
                EventTypes.ACCOUNT_RECORDED,
                "account",
                UUID.fromString("a2000000-0000-0000-0000-000000000001"),
                "{\"eventType\":\"AccountRecorded\"}");
        OutboxEvent removed = new OutboxEvent(
                MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY,
                EventTypes.ACCOUNT_REMOVED,
                "account",
                UUID.fromString("a2000000-0000-0000-0000-000000000002"),
                "{\"eventType\":\"AccountRemoved\"}");
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(recorded, removed));

        new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending();

        verify(rabbitTemplate).send(eq(MessagingTopology.EXCHANGE_NAME), eq(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY), org.mockito.ArgumentMatchers.any(Message.class));
        verify(rabbitTemplate).send(eq(MessagingTopology.EXCHANGE_NAME), eq(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY), org.mockito.ArgumentMatchers.any(Message.class));
        assertThat(recorded.getPublishedAt()).isNotNull();
        assertThat(removed.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPending_whenRabbitPublishFails_shouldLeaveEventUnpublished() {
        OutboxEvent outboxEvent = new OutboxEvent(
                MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY,
                EventTypes.ACCOUNT_RECORDED,
                "account",
                UUID.fromString("a2000000-0000-0000-0000-000000000003"),
                "{\"eventType\":\"account.recorded.v1\"}");
        RuntimeException failure = new RuntimeException("rabbit unavailable");
        when(outboxEventRepository.findPendingForRelay()).thenReturn(List.of(outboxEvent));
        doThrow(failure).when(rabbitTemplate).send(
                eq(MessagingTopology.EXCHANGE_NAME),
                eq(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY),
                org.mockito.ArgumentMatchers.any(Message.class));

        assertThatThrownBy(() -> new ScheduledOutboxRelay(outboxEventRepository, rabbitTemplate).publishPending())
                .isSameAs(failure);
        assertThat(outboxEvent.getPublishedAt()).isNull();
    }

    private boolean isJsonMessage(Message message) {
        return MessageProperties.CONTENT_TYPE_JSON.equals(message.getMessageProperties().getContentType());
    }

}
