package com.batu.plaid_adapter_service.strategy.impl.plaid_error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.strategy.PlaidErrorHandlerStrategy;
import com.plaid.client.model.PlaidError;


@Component("TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION")
public class TransactionsMutationHandlerStrategy implements PlaidErrorHandlerStrategy{

    @Override
    public void handle(PlaidError error) {
        throw new PlaidRetryableException(error.getErrorMessage(), HttpStatus.BAD_GATEWAY);
    }

}
