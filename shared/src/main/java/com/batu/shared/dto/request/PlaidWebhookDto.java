package com.batu.shared.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaidWebhookDto {

    @JsonProperty("webhook_type")
    private String webhookType;

    @JsonProperty("webhook_code")
    private String webhookCode;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("error")
    private PlaidWebhookErrorDto error;

    @JsonProperty("new_transactions")
    private Integer newTransactions;

    @JsonProperty("removed_transactions")
    private Integer removedTransactions;
}
