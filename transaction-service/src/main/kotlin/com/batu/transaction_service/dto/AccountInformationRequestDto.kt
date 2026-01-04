package com.batu.transaction_service.dto


import java.util.UUID

data class AccountInformationRequestDto(
    val accountIds: Set<UUID> = emptySet()
)