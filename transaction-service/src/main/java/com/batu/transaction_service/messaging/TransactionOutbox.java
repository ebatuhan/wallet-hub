package com.batu.transaction_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.transaction_service.entity.OutboxEvent;
import com.batu.transaction_service.repository.OutboxEventRepository;

import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionOutbox {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionOutbox(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void save(String routingKey, BaseEvent<?> event) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    routingKey,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getAggregateVersion(),
                    objectMapper.writeValueAsString(event)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize transaction outbox event", exception);
        }
    }
}
