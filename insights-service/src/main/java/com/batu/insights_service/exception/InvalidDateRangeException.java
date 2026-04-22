package com.batu.insights_service.exception;

import org.springframework.http.HttpStatus;

import com.batu.shared.exception.AbstractApplicationException;

public class InvalidDateRangeException extends AbstractApplicationException {
    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message, HttpStatus.BAD_REQUEST);
    }
}
