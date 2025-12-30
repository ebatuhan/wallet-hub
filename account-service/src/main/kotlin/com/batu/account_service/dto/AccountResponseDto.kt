package com.batu.account_service.dto

import java.util.UUID
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountResponseDto @JvmOverloads constructor(
    val accountId: UUID? = null,
    val connectionId: UUID,
    val userId: UUID,
    val externalId: String,
    val accountName: String,
    val accountType: String,
    val accountSubtype: String?,
    val accountMask: String,
    val currentBalance: BigDecimal,
    val availableBalance: BigDecimal,
    val isoCurrentCode: String,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
