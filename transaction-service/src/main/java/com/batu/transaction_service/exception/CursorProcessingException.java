package com.batu.transaction_service.exception;

import org.springframework.http.HttpStatus;

public class CursorProcessingException extends AbstractApplicationException {

    public CursorProcessingException(String message, Throwable cause) {
        super("CURSOR_PROCESSING_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
