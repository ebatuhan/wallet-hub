package com.batu.shared.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PlaidWebhookDto(

    @JsonProperty("webhook_type")
    val webhookType: String? = null,

    @JsonProperty("webhook_code")
    val webhookCode: String? = null,

    @JsonProperty("item_id")
    val itemId: String? = null,

    @JsonProperty("error")
    val error: PlaidWebhookErrorDto? = null,

    @JsonProperty("new_transactions")
    val newTransactions: Int? = null,

    @JsonProperty("removed_transactions")
    val removedTransactions: Int? = null
)