package com.batu.account_service.dto

import java.util.UUID

import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountViewDto(
    val accountId : UUID,
    val connectionId : UUID,
    val accountName : String,
    val currentBalance : BigDecimal,
    val accountType : String,
    val accountMask : String,
    val createdAt: LocalDateTime?
)