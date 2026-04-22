package com.batu.plaid_adapter_service.unit.factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.dto.PlaidWebhookErrorDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

class WebhookFactoryTest {

    @Test
    void execute_usesNamedWebhookStrategy() {
        WebhookStrategy syncStrategy = mock(WebhookStrategy.class);
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of(
                "SYNC_UPDATES_AVAILABLE", syncStrategy,
                "DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = new PlaidWebhookDto("TRANSACTIONS", "SYNC_UPDATES_AVAILABLE", "item-1", null, null, null);

        factory.execute(dto);

        verify(syncStrategy).handle(dto);
        verifyNoMoreInteractions(defaultStrategy);
    }

    @Test
    void execute_usesDefaultStrategyForUnknownWebhookCode() {
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of("DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = new PlaidWebhookDto("TRANSACTIONS", "UNKNOWN_CODE", "item-1", null, null, null);

        factory.execute(dto);

        verify(defaultStrategy).handle(dto);
    }

    @Test
    void execute_usesDefaultStrategyForMissingWebhookCode() {
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of("DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = new PlaidWebhookDto("TRANSACTIONS", null, "item-1", null, null, null);

        factory.execute(dto);

        verify(defaultStrategy).handle(dto);
    }

    @Test
    void execute_usesSpecificErrorStrategyForErrorWebhook() {
        WebhookStrategy loginRequiredStrategy = mock(WebhookStrategy.class);
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of(
                "ITEM_LOGIN_REQUIRED", loginRequiredStrategy,
                "DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = new PlaidWebhookDto(
                "ITEM",
                "ERROR",
                "item-1",
                new PlaidWebhookErrorDto("ITEM_LOGIN_REQUIRED", "Login required"),
                null,
                null);

        factory.execute(dto);

        verify(loginRequiredStrategy).handle(dto);
        verifyNoMoreInteractions(defaultStrategy);
    }

    @Test
    void execute_usesDefaultStrategyForErrorWebhookWithoutErrorBody() {
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of("DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = new PlaidWebhookDto("ITEM", "ERROR", "item-1", null, null, null);

        factory.execute(dto);

        verify(defaultStrategy).handle(dto);
    }
}
