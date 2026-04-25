package com.batu.account_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.batu.account_service.exception.AbstractApplicationException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

@RestControllerAdvice
public class ApplicationErrorAdvice extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationErrorAdvice.class);
    private static final String GENERIC_ERROR_MESSAGE = "Unexpected error occurred. Please try again later.";

    @ExceptionHandler(AbstractApplicationException.class)
    public ProblemDetail handleApplicationExceptions(AbstractApplicationException ex, WebRequest request) {
        if (ex.getHttpStatus().is5xxServerError()) {
            recordExceptionOnCurrentSpan(ex);
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(),
                ex.getMessage());

        problemDetail.setProperty("code", ex.getCode());

        
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setProperty("code", "BAD_REQUEST");
        return problemDetail;
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex) {
        recordExceptionOnCurrentSpan(ex);
        logger.error("Data access error", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_ERROR_MESSAGE);
        problemDetail.setProperty("code", "DATA_ACCESS_ERROR");
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        recordExceptionOnCurrentSpan(ex);
        logger.error("Unexpected error", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_ERROR_MESSAGE);
        problemDetail.setProperty("code", "UNEXPECTED_ERROR");
        return problemDetail;
    }

    private void recordExceptionOnCurrentSpan(Exception ex) {
        Span span = Span.current();
        span.recordException(ex);
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        span.setAttribute("error.type", ex.getClass().getName());
    }
}
