package com.batu.plaid_adapter_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlaidWebhookDto(
        @JsonProperty("webhook_type") String webhookType,
        @JsonProperty("webhook_code") String webhookCode,
        @JsonProperty("item_id") String itemId,
        @JsonProperty("error") PlaidWebhookErrorDto error,
        @JsonProperty("new_transactions") Integer newTransactions,
        @JsonProperty("removed_transactions") Integer removedTransactions) {
}
