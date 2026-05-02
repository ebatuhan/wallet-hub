package com.batu.plaid_adapter_service.messaging.outbox;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.outbox.EventMessagePublisher;

@Component
public class RabbitEventMessagePublisher implements EventMessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitEventMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String routingKey, String payload) {
        rabbitTemplate.send(MessagingTopology.EXCHANGE_NAME, routingKey, MessageBuilder
                .withBody(payload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build());
    }
}
