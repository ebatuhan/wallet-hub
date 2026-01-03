package com.batu.transaction_service.dto

import java.util.UUID
import com.batu.transaction_service.dto.TransactionPrimaryCategoryDto

data class TransactionDetailedCategoryDto(
    val transactionDetailedCategoryId: UUID?,
    val displayName: String,
    val detailedCode: String,
    val primaryCategory: TransactionPrimaryCategoryDto
)
