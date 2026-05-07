package com.batu.transaction_service.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.transaction_service.entity.InboxEvent;
import com.batu.transaction_service.entity.OutboxEvent;

class MessagingEntityTest {

    private static final UUID EVENT_ID = UUID.fromString("e8000000-0000-0000-0000-000000000001");
    private static final UUID AGGREGATE_ID = UUID.fromString("e8000000-0000-0000-0000-000000000002");

    @Test
    void outboxEventConstructor_shouldPopulateUnpublishedEventFields() {
        OutboxEvent event = outboxEvent();

        assertThat(event.getRoutingKey()).isEqualTo(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY);
        assertThat(event.getEventType()).isEqualTo(EventTypes.TRANSACTION_RECORDED);
        assertThat(event.getAggregateType()).isEqualTo("transaction");
        assertThat(event.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(event.getPayload()).isEqualTo("{\"eventType\":\"transaction.recorded.v1\"}");
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void markPublished_shouldSetPublishedTimestamp() {
        OutboxEvent event = outboxEvent();
        Instant before = Instant.now();

        event.markPublished();

        assertThat(event.getPublishedAt()).isBetween(before, Instant.now());
    }

    @Test
    void inboxEventConstructor_shouldPopulateEventIdAndProcessedTimestamp() {
        Instant before = Instant.now();

        InboxEvent event = new InboxEvent(EVENT_ID);

        assertThat(event.getEventId()).isEqualTo(EVENT_ID);
        assertThat(event.getProcessedAt()).isBetween(before, Instant.now());
    }

    private OutboxEvent outboxEvent() {
        return new OutboxEvent(
                MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                EventTypes.TRANSACTION_RECORDED,
                "transaction",
                AGGREGATE_ID,
                "{\"eventType\":\"transaction.recorded.v1\"}");
    }
}
