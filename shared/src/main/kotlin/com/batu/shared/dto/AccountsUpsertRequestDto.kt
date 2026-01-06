package com.batu.shared.dto

data class AccountsUpsertRequestDto(
    val accounts : List<AccountRequestDto> = emptyList()
)