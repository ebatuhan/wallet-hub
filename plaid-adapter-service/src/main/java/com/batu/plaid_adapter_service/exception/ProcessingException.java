package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

import com.batu.plaid_adapter_service.exception.AbstractApplicationException;

public class ProcessingException extends AbstractApplicationException {
    public ProcessingException(String code, String message, Throwable cause) {
        super(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
