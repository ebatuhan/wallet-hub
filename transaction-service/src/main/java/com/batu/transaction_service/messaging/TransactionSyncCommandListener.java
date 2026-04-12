package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.command.TransactionSyncCommand;
import com.batu.transaction_service.service.TransactionService;

@Component
public class TransactionSyncCommandListener {

    private final TransactionService transactionService;

    public TransactionSyncCommandListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_CREATE_QUEUE)
    public void onTransactionCreate(TransactionSyncCommand command) {
        transactionService.create(command);
    }

    @RabbitListener(queues = MessagingTopology.TRANSACTION_UPDATE_QUEUE)
    public void onTransactionUpdate(TransactionSyncCommand command) {
        transactionService.update(command);
    }
}
