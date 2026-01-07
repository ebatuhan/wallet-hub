package com.batu.shared.dto

import java.time.LocalDate
import java.math.BigDecimal
import java.util.UUID
import com.batu.shared.dto.TransactionDetailedCategoryRequestDto

data class TransactionRequestDto(
    val userId: UUID,
    val accountId: UUID,
    val externalId: String,
    val amount: BigDecimal,
    val isoCurrencyCode: String,
    val transactionName: String,
    val transactionType: String,
    val date: LocalDate,
    val is_pending: Boolean? = false,
    val paymentChannel: String,
    val detailedCategoryCode: String,
    val isActive: Boolean = true
)