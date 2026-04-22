package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

@Component("HISTORICAL_UPDATE")
public class HistoricalUpdateStrategy implements WebhookStrategy {

    @Override
    public void handle(PlaidWebhookDto dto) {
        // Transactions Sync relies on SYNC_UPDATES_AVAILABLE; this webhook is ignored.
    }
}
