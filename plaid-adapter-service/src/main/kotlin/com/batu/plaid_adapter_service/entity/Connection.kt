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
class Connection @JvmOverloads constructor(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "external_id", nullable = false, unique = true)
    val externalId: String,

    @Column(nullable = false)
    var accessToken: String,

    @Column(nullable = false)
    val institutionId: String,

    @Column(nullable = false)
    val institutionName: String, 
    
    @Column(nullable = false)
    var connectionStatus: String = "ACTIVE",

    var errorCode: String? = null,
    var lastCursor: String? = null
) {
    @Id
    @Column(name = "connection_id", nullable = false)
    @UuidGenerator
    val connectionId: UUID? = null

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime? = null

    @Column(nullable = false)
    @UpdateTimestamp
    val updatedAt: LocalDateTime? = null
}
