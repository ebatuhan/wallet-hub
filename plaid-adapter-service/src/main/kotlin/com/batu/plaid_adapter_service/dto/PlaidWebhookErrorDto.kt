package com.batu.plaid_adapter_service.dto

data class PlaidWebhookErrorDto(
    val errorCode: String? = null,
    val errorMessage: String? = null
)