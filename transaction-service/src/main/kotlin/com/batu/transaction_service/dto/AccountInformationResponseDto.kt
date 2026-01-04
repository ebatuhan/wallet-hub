package com.batu.transaction_service.dto

import java.util.UUID

data class AccountInformationResponseDto(
    val accountId : UUID,
    val accountName : String
)