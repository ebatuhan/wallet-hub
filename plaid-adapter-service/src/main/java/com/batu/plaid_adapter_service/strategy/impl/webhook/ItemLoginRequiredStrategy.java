package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.request.PlaidWebhookDto;

@Component("ITEM_LOGIN_REQUIRED")
public class ItemLoginRequiredStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;

    public ItemLoginRequiredStrategy(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public void handle(PlaidWebhookDto dto) {
        Connection connection = connectionService.readByExternalId(dto.getItemId());
        connectionService.markDisabled(connection.getConnectionId(), dto.getError().getErrorCode());
    }
}
