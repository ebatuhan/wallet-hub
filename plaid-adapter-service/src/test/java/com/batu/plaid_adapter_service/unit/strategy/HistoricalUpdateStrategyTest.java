package com.batu.plaid_adapter_service.unit.strategy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.strategy.impl.webhook.HistoricalUpdateStrategy;

class HistoricalUpdateStrategyTest {

    private final HistoricalUpdateStrategy strategy = new HistoricalUpdateStrategy();

    @Test
    void handle_isNoOp() {
        assertDoesNotThrow(() -> strategy.handle(
                new PlaidWebhookDto("TRANSACTIONS", "HISTORICAL_UPDATE", "item-1", null, null, null)));
    }
}
