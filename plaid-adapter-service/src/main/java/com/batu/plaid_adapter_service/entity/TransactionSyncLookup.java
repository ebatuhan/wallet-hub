package com.batu.plaid_adapter_service.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "plaid_transaction_lookup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionSyncLookup {

    @Id
    @UuidGenerator
    @Column(name = "lookup_id", nullable = false, updatable = false)
    private UUID lookupId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "plaid_transaction_id", nullable = false, unique = true)
    private String plaidTransactionId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TransactionSyncLookup(UUID connectionId, String plaidTransactionId, UUID transactionId) {
        this.connectionId = connectionId;
        this.plaidTransactionId = plaidTransactionId;
        this.transactionId = transactionId;
    }
}
