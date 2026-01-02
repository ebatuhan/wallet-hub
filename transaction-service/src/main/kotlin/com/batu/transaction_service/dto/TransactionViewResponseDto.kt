package com.batu.transaction_service.dto

import java.util.UUID
import java.math.BigDecimal

data class TransactionViewResponseDto (
    val transactionId : UUID,
    val amount : BigDecimal,
    val transactionName : String,
    val isoCurrencyCode : String,
    val categoryDisplayName : String,
    val detailedCategoryName : String
)