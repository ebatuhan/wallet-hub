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
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account implements Persistable<UUID> {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID accountId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

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

    public Account(UUID userId, String institutionName, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode, boolean isActive) {
        this(userId, institutionName, accountName, accountType, accountSubtype, accountMask, currentBalance,
                availableBalance, isoCurrencyCode, isActive, 0L);
    }

    public Account(UUID userId, String institutionName, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode, boolean isActive, long syncVersion) {
        this.userId = userId;
        this.institutionName = institutionName;
        this.accountName = accountName;
        this.accountType = accountType;
        this.accountSubtype = accountSubtype;
        this.accountMask = accountMask;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.isoCurrencyCode = isoCurrencyCode;
        this.isActive = isActive;
        this.syncVersion = syncVersion;
    }

    public Account(UUID accountId, UUID userId, String institutionName, String accountName,
            String accountType, String accountSubtype, String accountMask, BigDecimal currentBalance,
            BigDecimal availableBalance, String isoCurrencyCode, boolean isActive) {
        this(userId, institutionName, accountName, accountType, accountSubtype, accountMask, currentBalance,
                availableBalance, isoCurrencyCode, isActive);
        this.accountId = accountId;
    }

    public Account(UUID accountId, UUID userId, String institutionName, String accountName,
            String accountType, String accountSubtype, String accountMask, BigDecimal currentBalance,
            BigDecimal availableBalance, String isoCurrencyCode, boolean isActive, long syncVersion) {
        this(userId, institutionName, accountName, accountType, accountSubtype, accountMask, currentBalance,
                availableBalance, isoCurrencyCode, isActive, syncVersion);
        this.accountId = accountId;
    }

    @Override
    public UUID getId() {
        return accountId;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
