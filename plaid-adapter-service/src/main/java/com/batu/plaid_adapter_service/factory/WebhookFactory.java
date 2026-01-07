package com.batu.plaid_adapter_service.factory;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.PlaidWebhookDto;

@Component
public class WebhookFactory {

    private final Map<String, WebhookStrategy> strategies;

    public WebhookFactory(Map<String, WebhookStrategy> strategies) {
        this.strategies = strategies;
    }

    private WebhookStrategy getStrategy(String name) {
        return strategies.getOrDefault(name, strategies.get("DEFAULT"));
    }

    public void execute(PlaidWebhookDto dto) {
        String webhookCode = dto.getWebhookCode();

        var strategy = webhookCode.equals("ERROR")
                ? getStrategy(dto.getError().getErrorCode())
                : getStrategy(dto.getWebhookCode());

        strategy.handle(dto);
    }
}
