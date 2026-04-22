package com.batu.consent_service.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consent_connection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consent_connection_provider_id", columnNames = { "provider", "provider_connection_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentConnection {

    @Id
    @UuidGenerator
    @Column(name = "connection_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_connection_id", nullable = false)
    private UUID providerConnectionId;

    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public ConsentConnection(UUID userId,
            String provider,
            UUID providerConnectionId,
            String institutionId,
            String institutionName,
            String displayName,
            String status,
            Instant lastSyncedAt) {
        this.userId = userId;
        this.provider = provider;
        this.providerConnectionId = providerConnectionId;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.displayName = displayName;
        this.status = status;
        this.lastSyncedAt = lastSyncedAt;
    }
}
