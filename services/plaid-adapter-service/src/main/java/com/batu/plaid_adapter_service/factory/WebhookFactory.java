package com.batu.plaid_adapter_service.factory;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebhookFactory {

    private final Map<String, WebhookStrategy> strategies;

    private WebhookStrategy getStrategy(String name) {
        return strategies.getOrDefault(name, strategies.get("DEFAULT"));
    }

    public void execute(PlaidWebhookDto dto) {
        if (dto == null || dto.webhookCode() == null || dto.webhookCode().isBlank()) {
            getStrategy("DEFAULT").handle(dto);
            return;
        }

        String strategyName = dto.webhookCode();
        if ("ERROR".equals(dto.webhookCode())) {
            strategyName = dto.error() == null || dto.error().errorCode() == null
                    ? "DEFAULT"
                    : dto.error().errorCode();
        }

        var strategy = getStrategy(strategyName);

        strategy.handle(dto);
    }
}
