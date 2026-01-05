package com.batu.shared.dto

data class ExchangeTokenRequestDto
@JvmOverloads
constructor(
        val publicToken: String,
        val accountIds: List<String> = listOf(),
        val institutionId: String,
        val institutionName: String,
)
