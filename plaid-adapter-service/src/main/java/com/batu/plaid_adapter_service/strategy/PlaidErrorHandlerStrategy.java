package com.batu.plaid_adapter_service.strategy;

import com.plaid.client.model.PlaidError;

public interface PlaidErrorHandlerStrategy {
    void handle(PlaidError error);

}
