package com.batu.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @UuidGenerator
    @Column(name = "account_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID accountId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "account_subtype")
    private String accountSubtype = "";

    @Column(name = "account_mask", nullable = false)
    private String accountMask;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "available_balance", nullable = false)
    private BigDecimal availableBalance;

    @Column(name = "iso_currency_code", nullable = false)
    private String isoCurrencyCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public Account(UUID connectionId, UUID userId, String externalId, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode, boolean isActive) {
        this.connectionId = connectionId;
        this.userId = userId;
        this.externalId = externalId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.accountSubtype = accountSubtype;
        this.accountMask = accountMask;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.isoCurrencyCode = isoCurrencyCode;
        this.isActive = isActive;
    }
}
