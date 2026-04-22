package com.batu.plaid_adapter_service.unit.factory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.factory.PlaidErrorHandlerFactory;
import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;

class PlaidErrorHandlerFactoryTest {

    @Test
    void execute_usesSpecificStrategyWhenPresent() {
        PlaidErrorHandlerStrategy specificStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidErrorHandlerStrategy defaultStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidErrorHandlerFactory factory = new PlaidErrorHandlerFactory(Map.of(
                "RATE_LIMIT_EXCEEDED", specificStrategy,
                "DEFAULT_ERROR_HANDLER", defaultStrategy));
        PlaidError error = new PlaidError().errorCode("RATE_LIMIT_EXCEEDED");

        factory.execute(error);

        verify(specificStrategy).handle(error);
        verifyNoMoreInteractions(defaultStrategy);
    }

    @Test
    void execute_usesDefaultStrategyWhenErrorCodeUnknown() {
        PlaidErrorHandlerStrategy defaultStrategy = mock(PlaidErrorHandlerStrategy.class);
        PlaidErrorHandlerFactory factory = new PlaidErrorHandlerFactory(Map.of("DEFAULT_ERROR_HANDLER", defaultStrategy));
        PlaidError error = new PlaidError().errorCode("SOMETHING_NEW_FROM_PLAID");

        factory.execute(error);

        verify(defaultStrategy).handle(error);
    }
}
