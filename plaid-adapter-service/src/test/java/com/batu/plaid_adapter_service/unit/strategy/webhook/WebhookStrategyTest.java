package com.batu.plaid_adapter_service.unit.strategy.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.dto.PlaidWebhookErrorDto;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.strategy.impl.webhook.DefaultWebhookStrategy;
import com.batu.plaid_adapter_service.strategy.impl.webhook.HistoricalUpdateStrategy;
import com.batu.plaid_adapter_service.strategy.impl.webhook.ItemLoginRequiredStrategy;
import com.batu.plaid_adapter_service.strategy.impl.webhook.SyncUpdatesAvailableStrategy;
import com.batu.plaid_adapter_service.strategy.impl.webhook.UserPermissionRevokedStrategy;

@ExtendWith(MockitoExtension.class)
class WebhookStrategyTest {

    private static final UUID USER_ID = UUID.fromString("75000000-0000-0000-0000-000000000001");
    private static final UUID CONNECTION_ID = UUID.fromString("75000000-0000-0000-0000-000000000002");

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PlaidIntegrationService plaidIntegrationService;

    @Test
    void defaultWebhookStrategy_whenHandled_shouldDoNothing() {
        assertThatCode(() -> new DefaultWebhookStrategy().handle(webhook("ITEM", "UNKNOWN", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void historicalUpdateStrategy_whenHandled_shouldDoNothing() {
        assertThatCode(() -> new HistoricalUpdateStrategy().handle(webhook("TRANSACTIONS", "HISTORICAL_UPDATE", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void itemLoginRequiredStrategy_whenPlaidErrorExists_shouldDeactivateConnectionWithErrorCode() {
        Connection connection = connection();
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);
        PlaidWebhookDto dto = webhook("ITEM", "ERROR", new PlaidWebhookErrorDto("ITEM_LOGIN_REQUIRED", "login"));

        new ItemLoginRequiredStrategy(connectionService).handle(dto);

        verify(connectionService).deactivate(CONNECTION_ID, "ITEM_LOGIN_REQUIRED");
    }

    @Test
    void itemLoginRequiredStrategy_whenPlaidErrorMissing_shouldDeactivateConnectionWithWebhookCode() {
        Connection connection = connection();
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);
        PlaidWebhookDto dto = webhook("ITEM", "ITEM_LOGIN_REQUIRED", null);

        new ItemLoginRequiredStrategy(connectionService).handle(dto);

        verify(connectionService).deactivate(CONNECTION_ID, "ITEM_LOGIN_REQUIRED");
    }

    @Test
    void syncUpdatesAvailableStrategy_whenHandled_shouldSyncConnectionByExternalItemId() {
        Connection connection = connection();
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);

        new SyncUpdatesAvailableStrategy(connectionService, plaidIntegrationService)
                .handle(webhook("TRANSACTIONS", "SYNC_UPDATES_AVAILABLE", null));

        verify(plaidIntegrationService).syncConnection(CONNECTION_ID);
    }

    @Test
    void userPermissionRevokedStrategy_whenHandled_shouldRemoveConnectionWithWebhookCode() {
        Connection connection = connection();
        when(connectionService.readByExternalId("item-1")).thenReturn(connection);

        new UserPermissionRevokedStrategy(connectionService, plaidIntegrationService)
                .handle(webhook("ITEM", "USER_PERMISSION_REVOKED", null));

        verify(plaidIntegrationService).removeConnection(CONNECTION_ID, "USER_PERMISSION_REVOKED");
    }

    @Test
    void ignoredStrategies_whenHandled_shouldNotUseCollaborators() {
        new DefaultWebhookStrategy().handle(webhook("ITEM", "UNKNOWN", null));
        new HistoricalUpdateStrategy().handle(webhook("TRANSACTIONS", "HISTORICAL_UPDATE", null));

        verifyNoInteractions(connectionService, plaidIntegrationService);
    }

    private Connection connection() {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");
        ReflectionTestUtils.setField(connection, "connectionId", CONNECTION_ID);
        return connection;
    }

    private PlaidWebhookDto webhook(String type, String code, PlaidWebhookErrorDto error) {
        return new PlaidWebhookDto(type, code, "item-1", error, null, null);
    }
}
