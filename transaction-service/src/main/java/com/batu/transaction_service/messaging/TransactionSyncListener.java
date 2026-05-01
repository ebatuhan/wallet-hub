package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.transaction_service.service.TransactionService;

import io.micrometer.observation.annotation.Observed;

@Component
public class TransactionSyncListener {

    private final TransactionService transactionService;

    public TransactionSyncListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_SYNC_QUEUE)
    @Observed(name = "transactions.sync.consume", contextualName = "transactions consume sync")
    public void consume(TransactionRequestDto request) {
        transactionService.upsertFromSync(request);
    }
}
