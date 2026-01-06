package com.batu.shared.dto

import com.batu.shared.dto.TransactionPrimaryCategoryRequestDto

data class TransactionDetailedCategoryRequestDto(
    val displayName : String,
    val detailedCode : String,
    val transactionPrimaryCategoryRequestDto : TransactionPrimaryCategoryRequestDto
)