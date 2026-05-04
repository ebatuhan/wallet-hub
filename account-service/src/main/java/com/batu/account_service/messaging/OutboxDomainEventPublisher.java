package com.batu.account_service.messaging;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.account_service.entity.OutboxEvent;
import com.batu.account_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxDomainEventPublisher {
    private static final String SOURCE = "account-service";
    private static final String AGGREGATE = "account";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    public void publishAccountRecorded(AccountRecorded event) {
        publish(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY, EventTypes.ACCOUNT_RECORDED, event.getAccountId(), event);
    }

    public void publishAccountRemoved(AccountRemoved event) {
        publish(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY, EventTypes.ACCOUNT_REMOVED, event.getAccountId(), event);
    }

    private void publish(String routingKey, String eventType, UUID accountId, Object payload) {
        BaseEvent<?> event = BaseEvent.create(eventType, SOURCE, AGGREGATE, accountId, payload);

        try {
            outboxEventRepository.save(new OutboxEvent(
                    routingKey,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    jsonMapper.writeValueAsString(event)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist account outbox event", exception);
        }
    }
}
