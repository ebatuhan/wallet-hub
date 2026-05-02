package com.batu.shared.messaging.outbox;

import java.util.UUID;

public record OutboxMessage(UUID outboxEventId, String routingKey, String payload) {
}
