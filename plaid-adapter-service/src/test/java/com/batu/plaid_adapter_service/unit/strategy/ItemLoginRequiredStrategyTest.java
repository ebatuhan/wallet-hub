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
import com.batu.plaid_adapter_service.dto.PlaidWebhookErrorDto;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.strategy.impl.webhook.ItemLoginRequiredStrategy;

@ExtendWith(MockitoExtension.class)
class ItemLoginRequiredStrategyTest {

    @Mock
    private ConnectionService connectionService;

    @InjectMocks
    private ItemLoginRequiredStrategy strategy;

    @Test
    void handle_deactivatesConnectionWithPlaidErrorCode() {
        Connection connection = new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank");
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);

        PlaidWebhookErrorDto error = new PlaidWebhookErrorDto("ITEM_LOGIN_REQUIRED", "Login required");

        strategy.handle(new PlaidWebhookDto("ITEM", "ITEM_LOGIN_REQUIRED", "item-1", error, null, null));

        verify(connectionService).deactivate(connection.getConnectionId(), "ITEM_LOGIN_REQUIRED");
    }
}
