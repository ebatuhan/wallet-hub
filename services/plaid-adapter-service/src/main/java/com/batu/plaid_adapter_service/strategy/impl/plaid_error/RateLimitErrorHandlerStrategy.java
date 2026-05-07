package com.batu.plaid_adapter_service.strategy.impl.plaid_error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;

@Component("RATE_LIMIT_EXCEEDED")
public class RateLimitErrorHandlerStrategy implements PlaidErrorHandlerStrategy {

    @Override
    public RuntimeException toException(PlaidError error) {
        return new PlaidRetryableException("Service exceed rate limit on calling Plaid API.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
