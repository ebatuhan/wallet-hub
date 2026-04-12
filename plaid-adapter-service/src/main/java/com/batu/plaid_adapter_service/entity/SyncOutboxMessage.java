package com.batu.plaid_adapter_service.entity;

import java.time.Instant;
import java.util.UUID;

import com.batu.plaid_adapter_service.entity.enums.SyncOutboxMessageStatus;
import com.batu.plaid_adapter_service.entity.enums.SyncOutboxMessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sync_outbox_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncOutboxMessage {

    @Id
    @UuidGenerator
    @Column(name = "outbox_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID outboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private SyncOutboxMessageType messageType;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncOutboxMessageStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public SyncOutboxMessage(SyncOutboxMessageType messageType, String routingKey, String payload) {
        this.messageType = messageType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = SyncOutboxMessageStatus.PENDING;
    }

    public void markProcessing() {
        this.status = SyncOutboxMessageStatus.PROCESSING;
    }

    public void markPending() {
        this.status = SyncOutboxMessageStatus.PENDING;
    }

    public void markPublished(Instant publishedAt) {
        this.status = SyncOutboxMessageStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }
}
