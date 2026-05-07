package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

import lombok.RequiredArgsConstructor;

@Component("USER_PERMISSION_REVOKED")
@RequiredArgsConstructor
public class UserPermissionRevokedStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;
    private final PlaidIntegrationService plaidIntegrationService;

    @Override
    public void handle(PlaidWebhookDto dto) {
        plaidIntegrationService.removeConnection(connectionService.readByExternalId(dto.itemId()).getConnectionId(),
                dto.webhookCode());
    }
}
