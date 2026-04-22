package com.batu.shared.exception;

import org.springframework.http.HttpStatus;

public class CursorProcessingException extends AbstractApplicationException {

    public CursorProcessingException(String message, Throwable cause) {
        super("CURSOR_PROCESSING_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
