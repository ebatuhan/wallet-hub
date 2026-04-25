package com.batu.transaction_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.observation.annotation.Observed;

import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionPersistedEvent;

@Component
public class TransactionAnalyticsPublisher {

    private final RabbitTemplate rabbitTemplate;

    public TransactionAnalyticsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Observed(name = "analytics.publish.transaction-persisted", contextualName = "analytics publish transaction persisted")
    public void publishTransactionsPersisted(TransactionsPersistedDomainEvent event) {
        for (TransactionPersistedEvent transaction : event.transactions()) {
            rabbitTemplate.convertAndSend(
                    MessagingTopology.EXCHANGE_NAME,
                    MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY,
                    transaction);
        }
    }
}
