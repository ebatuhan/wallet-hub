package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.request.PlaidWebhookDto;

@Component("SYNC_UPDATES_AVAILABLE")
public class SyncUpdatesAvailableStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;

    public SyncUpdatesAvailableStrategy(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public void handle(PlaidWebhookDto dto) {
        //TODO
    }
}
