package com.batu.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class AbstractApplicationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    protected AbstractApplicationException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
