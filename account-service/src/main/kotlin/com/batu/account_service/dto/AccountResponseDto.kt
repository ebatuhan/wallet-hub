package com.batu.account_service.dto

import java.util.UUID
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountResponseDto(
    val accountId: UUID,
    val connectionId: UUID,
    val externalId: String,
    val accountName: String,
    val accountType: String,
    val accountSubtype: String?,
    val accountMask: String,
    val currentBalance: BigDecimal,
    val availableBalance: BigDecimal,
    val isoCurrentCode: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)
