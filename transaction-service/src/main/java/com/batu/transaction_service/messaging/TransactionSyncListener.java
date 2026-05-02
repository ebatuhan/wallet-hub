package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionObserved;

import io.micrometer.observation.annotation.Observed;

@Component
public class TransactionSyncListener {

    private final TransactionEventHandler transactionEventHandler;

    public TransactionSyncListener(TransactionEventHandler transactionEventHandler) {
        this.transactionEventHandler = transactionEventHandler;
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_SYNC_QUEUE)
    @Observed(name = "transactions.sync.consume", contextualName = "transactions consume sync")
    public void consume(BaseEvent<TransactionObserved> event) {
        transactionEventHandler.handleTransactionObserved(event);
    }
}
