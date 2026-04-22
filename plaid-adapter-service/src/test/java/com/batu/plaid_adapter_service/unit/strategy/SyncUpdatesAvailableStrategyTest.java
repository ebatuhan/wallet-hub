package com.batu.plaid_adapter_service.unit.strategy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.strategy.impl.webhook.SyncUpdatesAvailableStrategy;

@ExtendWith(MockitoExtension.class)
class SyncUpdatesAvailableStrategyTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PlaidIntegrationService plaidIntegrationService;

    @InjectMocks
    private SyncUpdatesAvailableStrategy strategy;

    @Test
    void handle_triggersConnectionSync() {
        Connection connection = new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank");
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);

        strategy.handle(new PlaidWebhookDto("TRANSACTIONS", "SYNC_UPDATES_AVAILABLE", "item-1", null, null, null));

        verify(plaidIntegrationService).syncConnection(connection.getConnectionId());
    }
}
