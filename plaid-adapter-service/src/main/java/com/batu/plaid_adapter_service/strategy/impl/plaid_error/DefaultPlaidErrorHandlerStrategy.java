package com.batu.plaid_adapter_service.strategy.impl.plaid_error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;

@Component("DEFAULT_ERROR_HANDLER")
public class DefaultPlaidErrorHandlerStrategy implements PlaidErrorHandlerStrategy {

    @Override
    public void handle(PlaidError error) {
        String displayMessage = error.getDisplayMessage() != null
                ? error.getDisplayMessage()
                : "An error occurred in Plaid";

        throw new PlaidClientException(displayMessage, HttpStatus.BAD_GATEWAY);
    }

}
