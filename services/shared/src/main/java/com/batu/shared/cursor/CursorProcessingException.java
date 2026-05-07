package com.batu.shared.cursor;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CursorProcessingException extends ResponseStatusException {

    public CursorProcessingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
