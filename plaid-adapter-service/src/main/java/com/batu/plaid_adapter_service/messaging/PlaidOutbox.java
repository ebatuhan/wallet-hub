package com.batu.plaid_adapter_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.ConnectionRemoved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.batu.shared.messaging.outbox.OutboxService;

@Component
public class PlaidOutbox {
    private static final String SOURCE = "plaid-adapter-service";

    private final OutboxService outboxService;

    public PlaidOutbox(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void accountObserved(AccountObserved account, long version) {
        outboxService.save(MessagingTopology.ACCOUNT_OBSERVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.ACCOUNT_OBSERVED,
                SOURCE,
                "account",
                account.getAccountId(),
                version,
                account));
    }

    public void transactionObserved(TransactionObserved transaction, long version) {
        outboxService.save(MessagingTopology.TRANSACTION_OBSERVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.TRANSACTION_OBSERVED,
                SOURCE,
                "transaction",
                transaction.getTransactionId(),
                version,
                transaction));
    }

    public void connectionRemoved(ConnectionRemoved connection, long version) {
        outboxService.save(MessagingTopology.CONNECTION_REMOVED_ROUTING_KEY, BaseEvent.create(
                EventTypes.CONNECTION_REMOVED,
                SOURCE,
                "connection",
                connection.getConnectionId(),
                version,
                connection));
    }
}
