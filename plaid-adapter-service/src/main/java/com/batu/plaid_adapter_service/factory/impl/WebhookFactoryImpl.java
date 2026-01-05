package com.batu.plaid_adapter_service.factory.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

@Component
public class WebhookFactoryImpl implements WebhookFactory {

    private final Map<String, WebhookStrategy> webhookStrategyMap;

    public WebhookFactoryImpl(Map<String, WebhookStrategy> webhookStrategyMap) {
        this.webhookStrategyMap = webhookStrategyMap;
    }

    public WebhookStrategy getWebhookStrategy(String webhookType) {
        WebhookStrategy strategy = webhookStrategyMap.get(webhookType);

        if (strategy == null) {
            throw new IllegalArgumentException("No handlers supported for webhook type: " + webhookType);
        }

        return strategy;
    }

    @Override
    public void executeHandling(PlaidWebhookDto payload) {
        WebhookStrategy webhookStrategy = getWebhookStrategy(payload.getWebhookType());

        webhookStrategy.handle(payload);
    }

}
