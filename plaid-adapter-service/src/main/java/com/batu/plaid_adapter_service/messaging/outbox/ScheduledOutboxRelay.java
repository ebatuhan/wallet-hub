package com.batu.plaid_adapter_service.messaging.outbox;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.MessagingTopology;

@Component
public class ScheduledOutboxRelay {
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public ScheduledOutboxRelay(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${wallet-hub.outbox.relay-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (var event : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            rabbitTemplate.send(MessagingTopology.EXCHANGE_NAME, event.getRoutingKey(), MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build());
            event.markPublished();
        }
    }
}
