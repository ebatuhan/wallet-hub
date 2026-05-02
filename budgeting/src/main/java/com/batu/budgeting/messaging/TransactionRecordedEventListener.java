package com.batu.budgeting.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.annotation.Observed;

import com.batu.budgeting.service.BudgetService;
import com.batu.budgeting.service.input.ApplyTransactionInput;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.inbox.InboxProcessor;

@Component
public class TransactionRecordedEventListener {

    private final BudgetService budgetService;
    private final InboxProcessor inboxProcessor;

    public TransactionRecordedEventListener(BudgetService budgetService, InboxProcessor inboxProcessor) {
        this.budgetService = budgetService;
        this.inboxProcessor = inboxProcessor;
    }

    @RabbitListener(queues = MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE)
    @Observed(name = "budgeting.consume.transaction-recorded", contextualName = "budgeting consume transaction recorded")
    @Transactional
    public void onTransactionRecorded(BaseEvent<TransactionRecorded> event) {
        inboxProcessor.process(event, () -> budgetService.applyTransaction(toInput(event.getPayload())));
    }

    private ApplyTransactionInput toInput(TransactionRecorded transaction) {
        return new ApplyTransactionInput(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getIsoCurrencyCode(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPrimaryCategoryId(),
                transaction.isActive());
    }
}
