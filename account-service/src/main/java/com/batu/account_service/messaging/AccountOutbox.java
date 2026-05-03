package com.batu.account_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.account_service.entity.OutboxEvent;
import com.batu.account_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

import tools.jackson.databind.ObjectMapper;

@Component
public class AccountOutbox {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccountOutbox(OutboxEventRepository outboxEventRepository) {
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
            throw new IllegalStateException("Unable to serialize account outbox event", exception);
        }
    }
}
