package com.batu.plaid_adapter_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlaidWebhookErrorDto(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_message") String errorMessage) {
}
