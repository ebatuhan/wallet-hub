package com.batu.shared.messaging;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {
    private UUID eventId;
    private String eventType;
    private String source;
    private UUID correlationId;
    private UUID causationId;
    private String aggregateType;
    private UUID aggregateId;
    private long aggregateVersion;
    private Instant occurredAt;
    private T payload;

    public static <T> BaseEvent<T> create(String eventType, String source, String aggregateType, UUID aggregateId,
            long aggregateVersion, T payload) {
        UUID eventId = UUID.randomUUID();
        return new BaseEvent<>(eventId, eventType, source, eventId, null, aggregateType, aggregateId, aggregateVersion,
                Instant.now(), payload);
    }

    public static <T> BaseEvent<T> causedBy(String eventType, String source, String aggregateType, UUID aggregateId,
            long aggregateVersion, T payload, BaseEvent<?> cause) {
        UUID eventId = UUID.randomUUID();
        UUID correlationId = cause.getCorrelationId() != null ? cause.getCorrelationId() : cause.getEventId();
        return new BaseEvent<>(eventId, eventType, source, correlationId, cause.getEventId(), aggregateType, aggregateId,
                aggregateVersion, Instant.now(), payload);
    }
}
