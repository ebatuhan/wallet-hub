package com.batu.plaid_adapter_service.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import java.util.UUID
import java.time.LocalDateTime
import org.hibernate.annotations.UuidGenerator
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "connection")
class Connection(
    @Id
    @Column(name = "connection_id", nullable = false)
    @UuidGenerator
    val connectionId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name="external_id", nullable = false, unique = true)
    val externalId: String,

    var accessToken: String,

    val institutionId: String,

    val institutionName: String,

    var connectionStatus: String,

    var errorCode: String? = null,

    var lastCursor: String? = null,

    @Column(nullable = false, updatable= false)
    @CreationTimestamp
    val createdAt: LocalDateTime? = null,

    @Column(nullable= false)
    @UpdateTimestamp
    val updatedAt: LocalDateTime? = null
)