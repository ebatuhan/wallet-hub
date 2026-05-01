package com.batu.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction implements Persistable<UUID> {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "iso_currency_code", nullable = false, length = 3)
    private String isoCurrencyCode;

    @Column(name = "transaction_name", nullable = false)
    private String transactionName;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "is_pending")
    private Boolean pending = Boolean.FALSE;

    @Column(name = "payment_channel", nullable = false)
    private String paymentChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detailed_category_id")
    private TransactionDetailedCategory detailedCategory;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sync_version", nullable = false)
    private long syncVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public Transaction(UUID userId, UUID accountId, BigDecimal amount, String isoCurrencyCode,
            String transactionName, String transactionType, LocalDate date, Boolean pending, String paymentChannel,
            TransactionDetailedCategory detailedCategory, boolean isActive) {
        this(userId, accountId, amount, isoCurrencyCode, transactionName, transactionType, date, pending, paymentChannel,
                detailedCategory, isActive, 0L);
    }

    public Transaction(UUID userId, UUID accountId, BigDecimal amount, String isoCurrencyCode,
            String transactionName, String transactionType, LocalDate date, Boolean pending, String paymentChannel,
            TransactionDetailedCategory detailedCategory, boolean isActive, long syncVersion) {
        this.userId = userId;
        this.accountId = accountId;
        this.amount = amount;
        this.isoCurrencyCode = isoCurrencyCode;
        this.transactionName = transactionName;
        this.transactionType = transactionType;
        this.date = date;
        this.pending = pending;
        this.paymentChannel = paymentChannel;
        this.detailedCategory = detailedCategory;
        this.isActive = isActive;
        this.syncVersion = syncVersion;
    }

    public Transaction(UUID transactionId, UUID userId, UUID accountId, BigDecimal amount,
            String isoCurrencyCode, String transactionName, String transactionType, LocalDate date, Boolean pending,
            String paymentChannel, TransactionDetailedCategory detailedCategory, boolean isActive) {
        this(userId, accountId, amount, isoCurrencyCode, transactionName, transactionType, date, pending,
                paymentChannel, detailedCategory, isActive);
        this.transactionId = transactionId;
    }

    public Transaction(UUID transactionId, UUID userId, UUID accountId, BigDecimal amount,
            String isoCurrencyCode, String transactionName, String transactionType, LocalDate date, Boolean pending,
            String paymentChannel, TransactionDetailedCategory detailedCategory, boolean isActive, long syncVersion) {
        this(userId, accountId, amount, isoCurrencyCode, transactionName, transactionType, date, pending,
                paymentChannel, detailedCategory, isActive, syncVersion);
        this.transactionId = transactionId;
    }

    @Override
    public UUID getId() {
        return transactionId;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
