package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PlaidRetryableException extends ResponseStatusException {

    public PlaidRetryableException(String message, HttpStatus httpStatus) {
        super(httpStatus, message);
    }

}
