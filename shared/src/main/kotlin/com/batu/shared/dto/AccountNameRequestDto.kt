package com.batu.shared.dto


import java.util.UUID
import com.fasterxml.jackson.annotation.JsonProperty

data class AccountNameRequestDto(
    @JsonProperty("accountIds") val accountIds: Set<UUID> = emptySet(),
)