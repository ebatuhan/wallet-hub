package com.batu.plaid_adapter_service.strategy;

import com.plaid.client.model.PlaidError;

public interface PlaidErrorHandlerStrategy {
    RuntimeException toException(PlaidError error);

}
