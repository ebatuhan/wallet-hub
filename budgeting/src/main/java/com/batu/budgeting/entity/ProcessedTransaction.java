package com.batu.budgeting.entity;

import java.math.BigDecimal;
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
@Table(name = "processed_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedTransaction {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID transactionId;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedTransaction(UUID transactionId, UUID budgetId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.budgetId = budgetId;
        this.amount = amount;
    }
}
