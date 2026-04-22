package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

import lombok.RequiredArgsConstructor;

@Component("ITEM_LOGIN_REQUIRED")
@RequiredArgsConstructor
public class ItemLoginRequiredStrategy implements WebhookStrategy {

    private final ConnectionService connectionService;

    @Override
    public void handle(PlaidWebhookDto dto) {
        connectionService.markDisabled(connectionService.readByExternalId(dto.itemId()).getConnectionId(),
                dto.error() == null ? dto.webhookCode() : dto.error().errorCode());
    }
}
