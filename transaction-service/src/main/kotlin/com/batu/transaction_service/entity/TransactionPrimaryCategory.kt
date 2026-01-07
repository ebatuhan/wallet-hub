package com.batu.transaction_service.entity

import jakarta.persistence.Id
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.UuidGenerator
import java.util.UUID


@Entity
class TransactionPrimaryCategory(
    @Column(unique = true, nullable = false)
    val categoryCode : String,

    @Column(unique = true, nullable = false)
    val displayName : String,

    val iconUrl : String? = "default"

){
    @Id
    @UuidGenerator
    @Column(name = "transaction_primary_category_id", nullable = false)
    val transactionPrimaryCategoryId: UUID? = null
}