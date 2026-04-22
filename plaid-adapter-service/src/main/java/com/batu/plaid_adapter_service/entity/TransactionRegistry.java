package com.batu.plaid_adapter_service.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transaction_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_registry_account_external", columnNames = { "account_registry_id",
                "external_transaction_id" }),
        @UniqueConstraint(name = "uk_transaction_registry_transaction_id", columnNames = { "transaction_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionRegistry {

    @Id
    @UuidGenerator
    @Column(name = "transaction_registry_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID transactionRegistryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_registry_id", nullable = false)
    private AccountRegistry accountRegistry;

    @Column(name = "external_transaction_id", nullable = false)
    private String externalTransactionId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    public TransactionRegistry(AccountRegistry accountRegistry, String externalTransactionId, UUID transactionId) {
        this.accountRegistry = accountRegistry;
        this.externalTransactionId = externalTransactionId;
        this.transactionId = transactionId;
    }
}
