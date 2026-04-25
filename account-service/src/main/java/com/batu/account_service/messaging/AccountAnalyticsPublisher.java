package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.observation.annotation.Observed;

import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountPersistedEvent;

@Component
public class AccountAnalyticsPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AccountAnalyticsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Observed(name = "analytics.publish.account-persisted", contextualName = "analytics publish account persisted")
    public void publishAccountsPersisted(AccountsPersistedDomainEvent event) {
        for (AccountPersistedEvent account : event.accounts()) {
            rabbitTemplate.convertAndSend(
                    MessagingTopology.EXCHANGE_NAME,
                    MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY,
                    account);
        }
    }
}
