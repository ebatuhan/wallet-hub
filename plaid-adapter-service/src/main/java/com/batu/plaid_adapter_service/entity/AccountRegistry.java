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
@Table(name = "account_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_registry_account_id", columnNames = { "account_id" }),
        @UniqueConstraint(name = "uk_account_registry_fingerprint", columnNames = { "fingerprint" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountRegistry {

    @Id
    @UuidGenerator
    @Column(name = "account_registry_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID accountRegistryId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", insertable = false, updatable = false)
    private Connection connection;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    public AccountRegistry(UUID connectionId, UUID accountId, String fingerprint) {
        this.connectionId = connectionId;
        this.accountId = accountId;
        this.fingerprint = fingerprint;
    }
}
