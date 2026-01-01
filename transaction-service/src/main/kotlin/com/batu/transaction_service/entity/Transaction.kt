package com.batu.transaction_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator

@Entity
@Table(name = "transactions")
class Transaction(
        @Column(name = "user_id", nullable = false) 
        val userId: UUID,

        @Column(unique = true, nullable = false) 
        val externalId: String,

        @Column(nullable = false) 
        var amount: BigDecimal,

        @Column(nullable = false, length = 3) 
        val isoCurrentCode: String,
    
        @Column(nullable = false) 
        val transactionName: String,

        @Column(nullable = false) 
        val transactionType: String,

        @Column(nullable = false) 
        val date: LocalDate,

        var is_pending: Boolean? = false,

        @Column(nullable = false) 
        val paymentChannel: String,

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "detailed_category_id")
        val detailedCategory: TransactionDetailedCategory
) {
    @Id
    @UuidGenerator
    @Column(name = "transaction_id", nullable = false)
    val transactionId: UUID? = null

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime? = null

    @Column(nullable = false) @UpdateTimestamp val updatedAt: LocalDateTime? = null
}
