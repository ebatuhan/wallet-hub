package com.batu.account_service.exception;

import org.springframework.http.HttpStatus;

import com.batu.account_service.exception.AbstractApplicationException;

public class ResourceNotFoundException extends AbstractApplicationException{
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
