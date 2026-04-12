package com.batu.insights_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends AbstractApplicationException {
    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message, HttpStatus.BAD_REQUEST);
    }
}
