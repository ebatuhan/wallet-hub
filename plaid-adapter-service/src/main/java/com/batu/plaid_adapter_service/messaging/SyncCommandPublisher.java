package com.batu.plaid_adapter_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.command.TransactionSyncCommand;

@Component
public class SyncCommandPublisher {

    private final RabbitTemplate rabbitTemplate;

    public SyncCommandPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAccountCreate(AccountSyncCommand command) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.COMMAND_EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_CREATE_ROUTING_KEY,
                command);
    }

    public void publishAccountUpdate(AccountSyncCommand command) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.COMMAND_EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_UPDATE_ROUTING_KEY,
                command);
    }

    public void publishTransactionCreate(TransactionSyncCommand command) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.COMMAND_EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_CREATE_ROUTING_KEY,
                command);
    }

    public void publishTransactionUpdate(TransactionSyncCommand command) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.COMMAND_EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_UPDATE_ROUTING_KEY,
                command);
    }
}
