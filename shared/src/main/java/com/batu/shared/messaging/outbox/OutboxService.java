package com.batu.shared.messaging.outbox;

import com.batu.shared.messaging.BaseEvent;

import tools.jackson.databind.ObjectMapper;

public class OutboxService {
    private final OutboxEventStore outboxEventStore;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventStore outboxEventStore, ObjectMapper objectMapper) {
        this.outboxEventStore = outboxEventStore;
        this.objectMapper = objectMapper;
    }

    public void save(String routingKey, BaseEvent<?> event) {
        try {
            outboxEventStore.save(
                    routingKey,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getAggregateVersion(),
                    objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }
}
