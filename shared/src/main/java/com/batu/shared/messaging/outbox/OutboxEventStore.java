package com.batu.shared.messaging.outbox;

import java.util.List;
import java.util.UUID;

public interface OutboxEventStore {
    void save(String routingKey, String eventType, String aggregateType, UUID aggregateId, long aggregateVersion,
            String payload);

    List<OutboxMessage> findPending(int limit);

    void markPublished(UUID outboxEventId);
}
