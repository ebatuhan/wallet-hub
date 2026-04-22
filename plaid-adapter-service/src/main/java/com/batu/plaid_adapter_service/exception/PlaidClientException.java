package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

import com.batu.shared.exception.AbstractApplicationException;

public class PlaidClientException extends AbstractApplicationException{

    public PlaidClientException(String message, HttpStatus httpStatus) {
        super("PLAID_CLIENT_ERROR", message, httpStatus);
    }

}
