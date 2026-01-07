package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.SyncService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.PlaidWebhookDto;

@Component("SYNC_UPDATES_AVAILABLE")
public class SyncUpdatesAvailableStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;
    private final SyncService syncService;

    public SyncUpdatesAvailableStrategy(ConnectionService connectionService, SyncService syncService) {
        this.connectionService = connectionService;
        this.syncService = syncService;
    }

    @Override
    public void handle(PlaidWebhookDto dto) {
        String conectionExternalId = dto.getItemId();
        Connection connection = connectionService.readByExternalId(conectionExternalId);

        syncService.SyncTransactionsAndAccounts(connection);
    }

}
