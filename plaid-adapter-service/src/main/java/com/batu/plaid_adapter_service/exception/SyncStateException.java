package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

public class SyncStateException extends AbstractApplicationException {
    public SyncStateException(String message) {
        super("SYNC_STATE_ERROR", message, HttpStatus.CONFLICT);
    }
}
