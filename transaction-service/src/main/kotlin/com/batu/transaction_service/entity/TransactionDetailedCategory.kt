package com.batu.transaction_service.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
class TransactionDetailedCategory(
    @Column(nullable = false)
    val displayName : String,

    @Column(nullable = false)
    val categoryCode : String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_category_id")
    val transactionPrimaryCategory : TransactionPrimaryCategory,

    val description : String? = ""

){
    @Id
    @UuidGenerator
    @Column(name = "transaction_detailed_category_id", nullable = false)
    val transactionDetailedCategoryId : UUID? = null
    
}