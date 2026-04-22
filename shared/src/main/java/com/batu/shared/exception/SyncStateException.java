package com.batu.shared.exception;

import org.springframework.http.HttpStatus;

public class SyncStateException extends AbstractApplicationException {

    public SyncStateException(String message) {
        super("SYNC_STATE_INVALID", message, HttpStatus.CONFLICT);
    }
}
