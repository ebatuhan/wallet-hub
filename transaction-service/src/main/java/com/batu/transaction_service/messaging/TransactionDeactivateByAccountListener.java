package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRemoved;

import io.micrometer.observation.annotation.Observed;

@Component
public class TransactionDeactivateByAccountListener {

    private final TransactionEventHandler transactionEventHandler;

    public TransactionDeactivateByAccountListener(TransactionEventHandler transactionEventHandler) {
        this.transactionEventHandler = transactionEventHandler;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_REMOVED_TRANSACTION_QUEUE)
    @Observed(name = "transactions.account-removed.consume", contextualName = "transactions consume account removed")
    public void consume(BaseEvent<AccountRemoved> event) {
        transactionEventHandler.handleAccountRemoved(event);
    }
}
