package com.batu.plaid_adapter_service.factory;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;

@Component
public class PlaidErrorHandlerFactory {

    private final Map<String, PlaidErrorHandlerStrategy> strategies;

    public PlaidErrorHandlerFactory(Map<String, PlaidErrorHandlerStrategy> strategies) {
        this.strategies = strategies;
    }

    private PlaidErrorHandlerStrategy getStrategy(String name) {
        return strategies.getOrDefault(name, strategies.get("DEFAULT_ERROR_HANDLER"));
    }

    public void execute(PlaidError plaidError) {
        var strategy = getStrategy(plaidError.getErrorCode());

        strategy.handle(plaidError);
    }
}
