package com.batu.plaid_adapter_service.unit.factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.dto.PlaidWebhookErrorDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

class WebhookFactoryTest {

    @Test
    void execute_whenWebhookCodeMatchesStrategy_shouldUseSpecificStrategy() {
        WebhookStrategy syncStrategy = mock(WebhookStrategy.class);
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of(
                "SYNC_UPDATES_AVAILABLE", syncStrategy,
                "DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = webhook("TRANSACTIONS", "SYNC_UPDATES_AVAILABLE", null);

        factory.execute(dto);

        verify(syncStrategy).handle(dto);
        verifyNoInteractions(defaultStrategy);
    }

    @ParameterizedTest
    @MethodSource("defaultWebhookCases")
    void execute_whenWebhookIsMissingBlankOrUnknown_shouldUseDefaultStrategy(String caseName, PlaidWebhookDto dto) {
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of("DEFAULT", defaultStrategy));

        factory.execute(dto);

        verify(defaultStrategy).handle(dto);
    }

    @Test
    void execute_whenErrorWebhookHasKnownPlaidErrorCode_shouldUseErrorCodeStrategy() {
        WebhookStrategy loginRequiredStrategy = mock(WebhookStrategy.class);
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of(
                "ITEM_LOGIN_REQUIRED", loginRequiredStrategy,
                "DEFAULT", defaultStrategy));
        PlaidWebhookDto dto = webhook("ITEM", "ERROR", new PlaidWebhookErrorDto("ITEM_LOGIN_REQUIRED", "login"));

        factory.execute(dto);

        verify(loginRequiredStrategy).handle(dto);
        verifyNoInteractions(defaultStrategy);
    }

    @ParameterizedTest
    @MethodSource("errorDefaultCases")
    void execute_whenErrorWebhookHasNoUsableErrorCode_shouldUseDefaultStrategy(String caseName, PlaidWebhookDto dto) {
        WebhookStrategy defaultStrategy = mock(WebhookStrategy.class);
        WebhookFactory factory = new WebhookFactory(Map.of("DEFAULT", defaultStrategy));

        factory.execute(dto);

        verify(defaultStrategy).handle(dto);
    }

    private static Stream<Arguments> defaultWebhookCases() {
        return Stream.of(
                Arguments.of("null dto", null),
                Arguments.of("null code", webhook("ITEM", null, null)),
                Arguments.of("blank code", webhook("ITEM", " ", null)),
                Arguments.of("unknown code", webhook("ITEM", "UNKNOWN", null)));
    }

    private static Stream<Arguments> errorDefaultCases() {
        return Stream.of(
                Arguments.of("missing error", webhook("ITEM", "ERROR", null)),
                Arguments.of("missing error code", webhook("ITEM", "ERROR", new PlaidWebhookErrorDto(null, "message"))),
                Arguments.of("blank error code", webhook("ITEM", "ERROR", new PlaidWebhookErrorDto("", "message"))));
    }

    private static PlaidWebhookDto webhook(String type, String code, PlaidWebhookErrorDto error) {
        return new PlaidWebhookDto(type, code, "item-1", error, null, null);
    }
}
