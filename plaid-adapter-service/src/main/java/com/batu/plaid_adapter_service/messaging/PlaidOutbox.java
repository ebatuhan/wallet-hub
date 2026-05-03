package com.batu.plaid_adapter_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.ConnectionRemoved;
import com.batu.shared.messaging.event.TransactionObserved;

import com.batu.plaid_adapter_service.entity.OutboxEvent;
import com.batu.plaid_adapter_service.repository.OutboxEventRepository;

import tools.jackson.databind.ObjectMapper;

@Component
public class PlaidOutbox {
    private static final String SOURCE = "plaid-adapter-service";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaidOutbox(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    public void accountObserved(AccountObserved account, long version) {
        save(MessagingTopology.ACCOUNT_OBSERVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.ACCOUNT_OBSERVED,
                SOURCE,
                "account",
                account.getAccountId(),
                version,
                account));
    }

    public void transactionObserved(TransactionObserved transaction, long version) {
        save(MessagingTopology.TRANSACTION_OBSERVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.TRANSACTION_OBSERVED,
                SOURCE,
                "transaction",
                transaction.getTransactionId(),
                version,
                transaction));
    }

    public void connectionRemoved(ConnectionRemoved connection, long version) {
        save(MessagingTopology.CONNECTION_REMOVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.CONNECTION_REMOVED,
                SOURCE,
                "connection",
                connection.getConnectionId(),
                version,
                connection));
    }

    private void save(String routingKey, BaseEvent<?> event) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    routingKey,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getAggregateVersion(),
                    objectMapper.writeValueAsString(event)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize Plaid outbox event", exception);
        }
    }
}
