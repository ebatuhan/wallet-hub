package com.batu.plaid_adapter_service.factory;

import com.batu.shared.dto.PlaidWebhookDto;

public interface WebhookFactory {
    void executeHandling(PlaidWebhookDto payload);
}
