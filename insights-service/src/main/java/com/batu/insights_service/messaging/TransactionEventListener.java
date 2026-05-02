package com.batu.insights_service.messaging;

import java.math.BigDecimal;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import io.micrometer.observation.annotation.Observed;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.inbox.InboxProcessor;

@Component
public class TransactionEventListener {
    private final TransactionInsightsService transactionInsightsService;
    private final InboxProcessor inboxProcessor;

    public TransactionEventListener(TransactionInsightsService transactionInsightsService, InboxProcessor inboxProcessor) {
        this.transactionInsightsService = transactionInsightsService;
        this.inboxProcessor = inboxProcessor;
    }

    @RabbitListener(queues=MessagingTopology.TRANSACTION_PERSISTED_QUEUE)
    @Observed(name = "insights.consume.transaction-recorded", contextualName = "insights consume transaction recorded")
    public void onTransactionRecorded(BaseEvent<TransactionRecorded> event){
        inboxProcessor.process(event, () -> {
            TransactionRecorded message = event.getPayload();
            boolean isOutflow = message.getAmount().signum() < 0;

            TransactionInsightRow txRow = new TransactionInsightRow(
                 message.getDate(),
                 message.getPrimaryCategoryId(),
                 message.getPaymentChannel(),
                 message.getAmount(),
                 isOutflow,
                 message.isActive(),
                 message.getIsoCurrencyCode(),
                 message.getUserId(),
                 message.getAccountId(),
                 message.getTransactionId(),
                 event.getOccurredAt()
            );

            transactionInsightsService.save(txRow);
        });
    }

}
