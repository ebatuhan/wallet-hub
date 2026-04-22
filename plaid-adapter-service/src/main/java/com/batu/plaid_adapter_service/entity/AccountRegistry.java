package com.batu.plaid_adapter_service.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_registry_connection_external", columnNames = { "connection_id",
                "external_account_id" }),
        @UniqueConstraint(name = "uk_account_registry_account_id", columnNames = { "account_id" })
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

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    public AccountRegistry(UUID connectionId, String externalAccountId, UUID accountId) {
        this.connectionId = connectionId;
        this.externalAccountId = externalAccountId;
        this.accountId = accountId;
    }
}
