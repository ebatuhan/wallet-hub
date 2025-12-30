package com.batu.account_service.config;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.batu.account_service.exception.AbstractApplicationException;

@RestControllerAdvice
public class ApplicationErrorAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AbstractApplicationException.class)
    public ProblemDetail handleApplicationExceptions(AbstractApplicationException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(),
                ex.getMessage());

        problemDetail.setProperty("code", ex.getCode());

        
        return problemDetail;
    }
}