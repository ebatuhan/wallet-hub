package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.request.PlaidWebhookDto;

@Component("USER_PERMISSION_REVOKED")
public class UserPermissionRevokedStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;
    private final PlaidIntegrationService plaidIntegrationService;

    public UserPermissionRevokedStrategy(ConnectionService connectionService,
            PlaidIntegrationService plaidIntegrationService) {
        this.connectionService = connectionService;
        this.plaidIntegrationService = plaidIntegrationService;
    }

    @Override
    public void handle(PlaidWebhookDto dto) {
        Connection connection = connectionService.readByExternalId(dto.getItemId());
        plaidIntegrationService.deactivateConnectionData(connection);
        connectionService.markRemoved(connection.getConnectionId(), dto.getWebhookCode());
    }
}
