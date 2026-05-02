package com.batu.plaid_adapter_service.messaging.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.OutboxEvent;
import com.batu.plaid_adapter_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.outbox.OutboxEventStore;
import com.batu.shared.messaging.outbox.OutboxMessage;

@Component
public class JpaOutboxEventStore implements OutboxEventStore {
    private final OutboxEventRepository outboxEventRepository;

    public JpaOutboxEventStore(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public void save(String routingKey, String eventType, String aggregateType, UUID aggregateId, long aggregateVersion,
            String payload) {
        outboxEventRepository.save(new OutboxEvent(routingKey, eventType, aggregateType, aggregateId, aggregateVersion,
                payload));
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        return outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
                .stream()
                .limit(limit)
                .map(event -> new OutboxMessage(event.getOutboxEventId(), event.getRoutingKey(), event.getPayload()))
                .toList();
    }

    @Override
    public void markPublished(UUID outboxEventId) {
        outboxEventRepository.findById(outboxEventId).ifPresent(event -> {
            event.markPublished();
            outboxEventRepository.save(event);
        });
    }
}
