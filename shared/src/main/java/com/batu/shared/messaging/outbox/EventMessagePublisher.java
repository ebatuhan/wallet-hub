package com.batu.shared.messaging.outbox;

public interface EventMessagePublisher {
    void publish(String routingKey, String payload);
}
