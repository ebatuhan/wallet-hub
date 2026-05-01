package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.dto.request.TransactionsDeactivateByAccountRequestDto;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.transaction_service.service.TransactionService;

import io.micrometer.observation.annotation.Observed;

@Component
public class TransactionDeactivateByAccountListener {

    private final TransactionService transactionService;

    public TransactionDeactivateByAccountListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_DEACTIVATE_BY_ACCOUNT_QUEUE)
    @Observed(name = "transactions.deactivate-by-account.consume", contextualName = "transactions consume deactivate by account")
    public void consume(TransactionsDeactivateByAccountRequestDto request) {
        transactionService.deactivateByAccountIdFromSync(request.getAccountId(), request.getSyncVersion());
    }
}
