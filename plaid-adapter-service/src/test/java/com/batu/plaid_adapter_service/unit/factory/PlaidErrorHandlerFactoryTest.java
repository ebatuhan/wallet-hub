package com.batu.plaid_adapter_service.unit.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.factory.PlaidErrorHandlerFactory;
import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;

class PlaidErrorHandlerFactoryTest {

    @Test
    void toException_whenSpecificStrategyExists_shouldUseMatchingStrategy() {
        PlaidErrorHandlerStrategy specificStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidErrorHandlerStrategy defaultStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidError error = new PlaidError().errorCode("RATE_LIMIT_EXCEEDED");
        RuntimeException expected = new IllegalStateException("rate limited");
        when(specificStrategy.toException(error)).thenReturn(expected);
        PlaidErrorHandlerFactory factory = new PlaidErrorHandlerFactory(Map.of(
                "RATE_LIMIT_EXCEEDED", specificStrategy,
                "DEFAULT_ERROR_HANDLER", defaultStrategy));

        RuntimeException result = factory.toException(error);

        assertThat(result).isSameAs(expected);
        verify(specificStrategy).toException(error);
    }

    @Test
    void toException_whenSpecificStrategyMissing_shouldUseDefaultStrategy() {
        PlaidErrorHandlerStrategy defaultStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidError error = new PlaidError().errorCode("UNKNOWN_ERROR");
        RuntimeException expected = new IllegalArgumentException("unknown");
        when(defaultStrategy.toException(error)).thenReturn(expected);
        PlaidErrorHandlerFactory factory = new PlaidErrorHandlerFactory(Map.of("DEFAULT_ERROR_HANDLER", defaultStrategy));

        RuntimeException result = factory.toException(error);

        assertThat(result).isSameAs(expected);
        verify(defaultStrategy).toException(error);
    }
}
