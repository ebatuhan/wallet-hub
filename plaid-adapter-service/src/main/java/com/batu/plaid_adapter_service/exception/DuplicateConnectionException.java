package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

public class DuplicateConnectionException extends AbstractApplicationException {

    public DuplicateConnectionException(String message) {
        super("DUPLICATE_CONNECTION", message, HttpStatus.CONFLICT);
    }
}
