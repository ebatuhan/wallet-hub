package com.batu.consent_service.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.batu.consent_service.exception.ResourceNotFoundException;

import feign.FeignException;

@RestControllerAdvice
public class ApplicationErrorAdvice {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleProviderException(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        String body = ex.contentUTF8();

        return ResponseEntity.status(status == null ? HttpStatus.BAD_GATEWAY : status)
                .body(body == null || body.isBlank() ? ex.getMessage() : body);
    }
}
