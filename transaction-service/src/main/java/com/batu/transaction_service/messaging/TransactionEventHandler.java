package com.batu.transaction_service.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRemoved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.shared.messaging.inbox.InboxProcessor;
import com.batu.shared.messaging.outbox.OutboxService;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.service.TransactionService;
import com.batu.transaction_service.service.input.RecordTransactionInput;

@Component
public class TransactionEventHandler {
    private static final String SOURCE = "transaction-service";
    private static final String TRANSACTION_AGGREGATE = "transaction";

    private final TransactionService transactionService;
    private final InboxProcessor inboxProcessor;
    private final OutboxService outboxService;

    public TransactionEventHandler(TransactionService transactionService, InboxProcessor inboxProcessor,
            OutboxService outboxService) {
        this.transactionService = transactionService;
        this.inboxProcessor = inboxProcessor;
        this.outboxService = outboxService;
    }

    @Transactional
    public void handleTransactionObserved(BaseEvent<TransactionObserved> event) {
        inboxProcessor.process(event, () -> transactionService.recordTransaction(toInput(event))
                .ifPresent(transaction -> {
                    if (transaction.isActive()) {
                        saveTransactionRecorded(transaction, event);
                    } else {
                        saveTransactionRemoved(transaction, event);
                    }
                }));
    }

    @Transactional
    public void handleAccountRemoved(BaseEvent<AccountRemoved> event) {
        inboxProcessor.process(event, () -> transactionService
                .deactivateByAccountId(event.getPayload().getAccountId(), event.getAggregateVersion())
                .forEach(transaction -> saveTransactionRemoved(transaction, event)));
    }

    private RecordTransactionInput toInput(BaseEvent<TransactionObserved> event) {
        TransactionObserved payload = event.getPayload();
        return new RecordTransactionInput(
                payload.getTransactionId(),
                payload.getUserId(),
                payload.getAccountId(),
                payload.getAmount(),
                payload.getIsoCurrencyCode(),
                payload.getTransactionName(),
                payload.getTransactionType(),
                payload.getDate(),
                payload.getPending(),
                payload.getPaymentChannel(),
                payload.getDetailedCategoryCode(),
                payload.isActive(),
                event.getAggregateVersion());
    }

    private void saveTransactionRecorded(Transaction transaction, BaseEvent<?> cause) {
        TransactionRecorded payload = new TransactionRecorded(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getIsoCurrencyCode(),
                transaction.getTransactionName(),
                transaction.getTransactionType(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPaymentChannel(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode(),
                transaction.isActive());

        outboxService.save(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY, BaseEvent.causedBy(
                EventTypes.TRANSACTION_RECORDED,
                SOURCE,
                TRANSACTION_AGGREGATE,
                transaction.getTransactionId(),
                transaction.getSyncVersion(),
                payload,
                cause));
    }

    private void saveTransactionRemoved(Transaction transaction, BaseEvent<?> cause) {
        TransactionRemoved payload = new TransactionRemoved(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAccountId());

        outboxService.save(MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY, BaseEvent.causedBy(
                EventTypes.TRANSACTION_REMOVED,
                SOURCE,
                TRANSACTION_AGGREGATE,
                transaction.getTransactionId(),
                transaction.getSyncVersion(),
                payload,
                cause));
    }
}
