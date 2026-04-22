package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

import com.batu.shared.exception.AbstractApplicationException;

public class PlaidRetryableException extends AbstractApplicationException{

    public PlaidRetryableException(String message, HttpStatus httpStatus) {
        super("PLAID_RETRYABLE_EXCEPTION", message, httpStatus);
    }

}
