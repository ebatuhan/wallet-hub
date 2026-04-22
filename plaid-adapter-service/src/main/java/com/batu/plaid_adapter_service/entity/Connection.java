package com.batu.plaid_adapter_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.batu.plaid_adapter_service.entity.enums.ProviderType;

@Entity
@Table(name = "connection")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Connection {

    @Id
    @UuidGenerator
    @Column(name = "connection_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false)
    private String provider = ProviderType.PLAID.name();

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "connection_status", nullable = false)
    private String connectionStatus = "ACTIVE";

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "last_cursor")
    private String lastCursor;

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

    public Connection(UUID userId, String externalId, String accessToken, String institutionId, String institutionName) {
        this.userId = userId;
        this.externalId = externalId;
        this.accessToken = accessToken;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.displayName = institutionName;
    }

    public Connection(UUID userId, String externalId, String accessToken, String institutionId, String institutionName,
            String connectionStatus, String errorCode, String lastCursor) {
        this(userId, externalId, accessToken, institutionId, institutionName);
        this.connectionStatus = connectionStatus;
        this.errorCode = errorCode;
        this.lastCursor = lastCursor;
    }
}
