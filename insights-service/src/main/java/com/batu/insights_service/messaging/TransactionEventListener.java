package com.batu.insights_service.messaging;

import java.math.BigDecimal;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionPersistedEvent;

@Component
public class TransactionEventListener {
    private final TransactionInsightsService transactionInsightsService;

    public TransactionEventListener(TransactionInsightsService transactionInsightsService) {
        this.transactionInsightsService = transactionInsightsService;
    }

    @RabbitListener(queues=MessagingTopology.TRANSACTION_PERSISTED_QUEUE)
    public void onTransactionPersisted(TransactionPersistedEvent message){
        boolean isOutflow = message.getAmount().signum() < 0;

        TransactionInsightRow txRow = new TransactionInsightRow(
             message.getDate(),
             message.getPrimaryCategoryCode(),
             message.getPaymentChannel(),
             message.getAmount(),
             isOutflow,
             message.isActive(),
             message.getIsoCurrencyCode(),
             message.getUserId(),
             message.getAccountId(),
             message.getTransactionId(),
             message.getOccurredAt()
        );

        transactionInsightsService.save(txRow);
    }

}
