package com.batu.shared.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import feign.FeignException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonApplicationErrorAdvice extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommonApplicationErrorAdvice.class);
    private static final String GENERIC_ERROR_MESSAGE = "Unexpected error occurred. Please try again later.";
    private static final String DOWNSTREAM_ERROR_MESSAGE = "Downstream service request failed.";
    private static final Pattern UPSTREAM_MESSAGE_PATTERN = Pattern.compile(
            "\\\"(?:detail|message|error)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            recordExceptionOnCurrentSpan(ex);
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        problemDetail.setProperty("message", ex.getReason());
        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed");

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed");

        Map<String, String> errors = new HashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            for (var error : result.getResolvableErrors()) {
                errors.put(parameterName == null ? "request" : parameterName, error.getDefaultMessage());
            }
        }
        problemDetail.setProperty("errors", errors);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex) {
        recordExceptionOnCurrentSpan(ex);
        logger.error("Data access error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_ERROR_MESSAGE);
    }

    @ExceptionHandler(FeignException.class)
    public ProblemDetail handleFeignException(FeignException ex) {
        HttpStatusCode status = resolveFeignStatus(ex);
        if (status.is5xxServerError()) {
            recordExceptionOnCurrentSpan(ex);
            logger.error("Downstream service error", ex);
        }

        String detail = status.is5xxServerError()
                ? DOWNSTREAM_ERROR_MESSAGE
                : upstreamDetail(ex).orElse(DOWNSTREAM_ERROR_MESSAGE);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("message", detail);
        problemDetail.setProperty("upstreamStatus", ex.status());
        return problemDetail;
    }

    private HttpStatusCode resolveFeignStatus(FeignException ex) {
        HttpStatusCode upstreamStatus = HttpStatusCode.valueOf(ex.status());
        if (upstreamStatus.is5xxServerError()) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (upstreamStatus.is4xxClientError()) {
            return upstreamStatus;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private Optional<String> upstreamDetail(FeignException ex) {
        String body = ex.contentUTF8();
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = UPSTREAM_MESSAGE_PATTERN.matcher(body);
        if (matcher.find() && !matcher.group(1).isBlank()) {
            return Optional.of(matcher.group(1));
        }
        if (body.length() <= 500 && !body.startsWith("{")) {
            return Optional.of(body);
        }
        return Optional.empty();
    }

    private void recordExceptionOnCurrentSpan(Exception ex) {
        Span span = Span.current();
        span.recordException(ex);
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        span.setAttribute("error.type", ex.getClass().getName());
    }

}
