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
import com.batu.plaid_adapter_service.strategy.impl.webhook.UserPermissionRevokedStrategy;

@ExtendWith(MockitoExtension.class)
class UserPermissionRevokedStrategyTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PlaidIntegrationService plaidIntegrationService;

    @InjectMocks
    private UserPermissionRevokedStrategy strategy;

    @Test
    void handle_triggersConnectionRemoval() {
        Connection connection = new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank");
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);

        strategy.handle(new PlaidWebhookDto("ITEM", "USER_PERMISSION_REVOKED", "item-1", null, null, null));

        verify(plaidIntegrationService).removeConnection(connection.getConnectionId(), "USER_PERMISSION_REVOKED");
    }
}
