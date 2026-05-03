package com.batu.transaction_service.messaging;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.transaction_service.entity.OutboxEvent;
import com.batu.transaction_service.repository.OutboxEventRepository;

import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxDomainEventPublisher {
    private static final String SOURCE = "transaction-service";
    private static final String AGGREGATE = "transaction";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void publishTransactionRecorded(TransactionRecorded event) {
        publish(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY, EventTypes.TRANSACTION_RECORDED,
                event.getTransactionId(), event);
    }

    public void publishTransactionRemoved(TransactionRemoved event) {
        publish(MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY, EventTypes.TRANSACTION_REMOVED,
                event.getTransactionId(), event);
    }

    private void publish(String routingKey, String eventType, UUID transactionId, Object payload) {
        BaseEvent<?> event = BaseEvent.create(eventType, SOURCE, AGGREGATE, transactionId, payload);

        try {
            outboxEventRepository.save(new OutboxEvent(
                    routingKey,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    objectMapper.writeValueAsString(event)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist transaction outbox event", exception);
        }
    }
}
