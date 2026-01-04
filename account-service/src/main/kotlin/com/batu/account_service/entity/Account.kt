package com.batu.account_service.entity

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.UUID
import java.time.LocalDateTime
import java.math.BigDecimal

@Entity
@Table(name = "accounts")

class Account( 
    @Column(name = "connection_id", nullable = false)
    val connectionId : UUID,

    @Column(name = "user_id", nullable = false)
    val userId : UUID,
    
    @Column(unique = true, nullable = false)
    val externalId : String,

    @Column(nullable = false)
    var accountName : String,

    @Column(nullable = false)
    var accountType : String,

    @Column
    var accountSubtype : String,

    @Column(nullable = false)
    var accountMask : String, 

    @Column(nullable = false)
    var currentBalance : BigDecimal,

    @Column(nullable = false)
    var availableBalance : BigDecimal,

    @Column(nullable = false)
    var isoCurrencyCode : String,

    @Column
    var isActive : Boolean = true
    
) { 

    @Id
    @Column(nullable = false)
    @UuidGenerator
    val accountId : UUID? = null 

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime? = null 

    @Column(nullable = false)
    @UpdateTimestamp
    val updatedAt: LocalDateTime? = null
}