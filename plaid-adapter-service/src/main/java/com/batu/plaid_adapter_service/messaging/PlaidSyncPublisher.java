package com.batu.plaid_adapter_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.request.TransactionsDeactivateByAccountRequestDto;
import com.batu.shared.dto.request.AccountDeactivateRequestDto;
import com.batu.shared.messaging.MessagingTopology;

import io.micrometer.observation.annotation.Observed;

@Component
public class PlaidSyncPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PlaidSyncPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Observed(name = "plaid.sync.publish-account", contextualName = "plaid publish account sync")
    public void publishAccount(AccountRequestDto request) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_SYNC_ROUTING_KEY,
                request);
    }

    @Observed(name = "plaid.sync.publish-transaction", contextualName = "plaid publish transaction sync")
    public void publishTransaction(TransactionRequestDto request) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_SYNC_ROUTING_KEY,
                request);
    }

    @Observed(name = "plaid.remove.publish-account", contextualName = "plaid publish account deactivate")
    public void publishAccountDeactivate(AccountDeactivateRequestDto request) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.ACCOUNT_DEACTIVATE_ROUTING_KEY,
                request);
    }

    @Observed(name = "plaid.remove.publish-transactions", contextualName = "plaid publish transactions deactivate")
    public void publishTransactionsDeactivateByAccount(TransactionsDeactivateByAccountRequestDto request) {
        rabbitTemplate.convertAndSend(
                MessagingTopology.EXCHANGE_NAME,
                MessagingTopology.TRANSACTION_DEACTIVATE_BY_ACCOUNT_ROUTING_KEY,
                request);
    }
}
