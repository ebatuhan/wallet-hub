package com.batu.plaid_adapter_service.exception;

import org.springframework.http.HttpStatus;

public abstract class AbstractApplicationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    protected AbstractApplicationException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
