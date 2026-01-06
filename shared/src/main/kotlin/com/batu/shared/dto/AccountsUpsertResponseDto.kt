package com.batu.shared.dto

import java.util.UUID

data class AccountsUpsertResponseDto(
    val insertedAccountsMap : Map<String, UUID> = emptyMap()
){

}