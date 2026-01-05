package com.batu.account_service.dto


import java.util.UUID
import com.fasterxml.jackson.annotation.JsonProperty

data class AccountInformationRequestDto(
    val accountIds: Set<UUID> = emptySet()
)