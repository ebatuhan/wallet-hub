package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRemoved;
import com.batu.transaction_service.service.TransactionService;

import io.micrometer.observation.annotation.Observed;

@Component
public class AccountRemovedEventListener {

    private final TransactionService transactionService;

    public AccountRemovedEventListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_ACCOUNT_REMOVED_QUEUE)
    @Observed(name = "transaction.consume.account-removed", contextualName = "transaction consume account removed")
    @Transactional
    public void onAccountRemoved(BaseEvent<AccountRemoved> event) {
        transactionService.deactivateTransactionsByAccountId(event.getPayload().getAccountId());
    }
}
