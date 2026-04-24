package com.batu.transaction_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCursorException extends AbstractApplicationException {

    public InvalidCursorException(String message, Throwable cause) {
        super("INVALID_CURSOR", message, HttpStatus.BAD_REQUEST);
        initCause(cause);
    }
}
