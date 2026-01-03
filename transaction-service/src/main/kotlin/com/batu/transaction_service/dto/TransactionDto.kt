package com.batu.transaction_service.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import com.batu.transaction_service.dto.TransactionDetailedCategoryDto

data class TransactionDto(
    val transactionId: UUID?,
    val userId: UUID,
    val externalId: String,
    val amount: BigDecimal,
    val isoCurrencyCode: String,
    val transactionName: String,
    val transactionType: String,
    val date: LocalDate,
    val isPending: Boolean?,
    val paymentChannel: String,
    val detailedCategory: TransactionDetailedCategoryDto,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)


