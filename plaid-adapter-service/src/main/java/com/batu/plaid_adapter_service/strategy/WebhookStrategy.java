package com.batu.plaid_adapter_service.strategy;

import com.batu.shared.dto.request.PlaidWebhookDto;

public interface WebhookStrategy {
    void handle(PlaidWebhookDto dto);

}
