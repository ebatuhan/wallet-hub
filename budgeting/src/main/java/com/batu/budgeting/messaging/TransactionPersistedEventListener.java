package com.batu.budgeting.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.budgeting.service.BudgetService;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionPersistedEvent;

@Component
public class TransactionPersistedEventListener {

    private final BudgetService budgetService;

    public TransactionPersistedEventListener(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @RabbitListener(queues = MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE)
    public void onTransactionPersisted(TransactionPersistedEvent event) {
        budgetService.applyTransactionEvent(event);
    }
}
