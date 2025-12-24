package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AbstractApplicationException{
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
