package com.batu.account_service.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxEvent {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant processedAt = Instant.now();

    public InboxEvent(UUID eventId) {
        this.eventId = eventId;
    }
}
