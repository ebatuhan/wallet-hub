package com.batu.shared.messaging.outbox;

public class OutboxRelay {
    private final OutboxEventStore outboxEventStore;
    private final EventMessagePublisher eventMessagePublisher;

    public OutboxRelay(OutboxEventStore outboxEventStore, EventMessagePublisher eventMessagePublisher) {
        this.outboxEventStore = outboxEventStore;
        this.eventMessagePublisher = eventMessagePublisher;
    }

    public void publishPending() {
        for (OutboxMessage message : outboxEventStore.findPending(100)) {
            eventMessagePublisher.publish(message.routingKey(), message.payload());
            outboxEventStore.markPublished(message.outboxEventId());
        }
    }
}
