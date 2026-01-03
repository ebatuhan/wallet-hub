
package com.batu.transaction_service.dto
import java.util.UUID

data class TransactionPrimaryCategoryDto(
    val transactionPrimaryCategoryId: UUID?,
    val categoryCode: String,
    val displayName: String,
    val iconUrl: String?
)