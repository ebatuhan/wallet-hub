package com.batu.plaid_adapter_service.strategy;

import com.batu.shared.dto.PlaidWebhookDto;

public interface WebhookStrategy {
    void handle(PlaidWebhookDto request);
}
