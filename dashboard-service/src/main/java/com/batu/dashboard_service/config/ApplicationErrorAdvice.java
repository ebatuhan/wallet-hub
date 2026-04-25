package com.batu.dashboard_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

@RestControllerAdvice
public class ApplicationErrorAdvice extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationErrorAdvice.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        recordExceptionOnCurrentSpan(ex);
        logger.error("Unexpected error", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred. Please try again later.");
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
