package com.batu.budgeting.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.annotation.Observed;

import com.batu.budgeting.service.BudgetService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;

@Component
public class TransactionRecordedEventListener {

    private final BudgetService budgetService;
    private final BudgetingInbox budgetingInbox;

    public TransactionRecordedEventListener(BudgetService budgetService, BudgetingInbox budgetingInbox) {
        this.budgetService = budgetService;
        this.budgetingInbox = budgetingInbox;
    }

    @RabbitListener(queues = MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE)
    @Observed(name = "budgeting.consume.transaction-recorded", contextualName = "budgeting consume transaction recorded")
    @Transactional
    public void onTransactionRecorded(BaseEvent<TransactionRecorded> event) {
        budgetingInbox.process(event, () -> budgetService.applyTransaction(event.getPayload()));
    }
}
