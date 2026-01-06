package com.batu.shared.dto

import java.util.UUID

data class AccountNameResponseDto(
    val accountId : UUID,
    val accountName : String
)