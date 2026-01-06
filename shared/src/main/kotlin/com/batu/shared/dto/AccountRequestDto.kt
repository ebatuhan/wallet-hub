package com.batu.shared.dto

import java.util.UUID
import java.time.LocalDateTime
import java.math.BigDecimal


data class AccountRequestDto(
    val connectionId : UUID,
    val userId : UUID,
    val externalId : String,
    val accountName : String,
    val accountType : String,
    val accountSubtype : String,
    val accountMask : String,
    val currentBalance : BigDecimal,
    val availableBalance : BigDecimal,
    val isoCurrencyCode : String,
    val isActive : Boolean? = true
){

}