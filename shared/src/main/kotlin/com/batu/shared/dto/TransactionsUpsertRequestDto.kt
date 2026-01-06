package com.batu.shared.dto 

data class TransactionsUpsertRequestDto(
    val transactions : List<TransactionRequestDto> = emptyList()
)