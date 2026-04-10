package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.shared.messaging.saga.InitialTransactionsPersistedEvent;
import com.batu.shared.messaging.saga.PersistInitialTransactionsCommand;
import com.batu.shared.messaging.saga.SagaChannels;
import com.batu.transaction_service.service.TransactionService;

@Component
public class InitialTransactionsPersistenceListener {

    private final TransactionService transactionService;
    private final RabbitTemplate rabbitTemplate;

    public InitialTransactionsPersistenceListener(TransactionService transactionService, RabbitTemplate rabbitTemplate) {
        this.transactionService = transactionService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = SagaChannels.PERSIST_INITIAL_TRANSACTIONS_COMMAND_QUEUE)
    public void persistInitialTransactions(PersistInitialTransactionsCommand command) {
        transactionService.batchUpsertTransactions(new TransactionsUpsertRequestDto(command.getTransactions()));

        rabbitTemplate.convertAndSend(
                SagaChannels.EXCHANGE,
                SagaChannels.INITIAL_TRANSACTIONS_PERSISTED_EVENT_KEY,
                new InitialTransactionsPersistedEvent(
                        command.getSagaId(),
                        command.getConnectionId(),
                        command.getNextCursor()));
    }
}
