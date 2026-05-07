package com.batu.shared.unit.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import com.batu.shared.error.CommonApplicationErrorAdvice;

class CommonApplicationErrorAdviceTest {

    private final TestableCommonApplicationErrorAdvice advice = new TestableCommonApplicationErrorAdvice();

    @Test
    void handleResponseStatusException_whenNotFound_shouldReturnProblemDetailWithMessage() {
        ProblemDetail problemDetail = advice.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Account not found");
        assertThat(problemDetail.getProperties()).containsEntry("message", "Account not found");
    }

    @Test
    void handleMethodArgumentNotValid_whenFieldErrorsExist_shouldReturnErrorsMap() throws Exception {
        MethodArgumentNotValidException exception = validationExceptionWithFieldError(
                "limitAmount", "must be greater than 0");

        ResponseEntity<Object> response = advice.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getDetail()).isEqualTo("Request validation failed");
        assertThat(problemDetail.getProperties()).containsKey("errors");
        assertThat(problemDetail.getProperties().get("errors"))
                .isEqualTo(Map.of("limitAmount", "must be greater than 0"));
    }

    @Test
    void handleIllegalArgumentException_whenRaised_shouldReturnBadRequestProblemDetail() {
        ProblemDetail problemDetail = advice.handleIllegalArgumentException(new IllegalArgumentException("Invalid cursor"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid cursor");
    }

    @Test
    void handleDataAccessException_whenRaised_shouldHideInternalDetails() {
        ProblemDetail problemDetail = advice.handleDataAccessException(
                new DataRetrievalFailureException("duplicate key value violates unique constraint account_external_id_key"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Unexpected error occurred. Please try again later.");
        assertThat(problemDetail.getDetail()).doesNotContain("account_external_id_key");
    }

    private MethodArgumentNotValidException validationExceptionWithFieldError(String field, String message) throws Exception {
        Method method = SampleController.class.getDeclaredMethod("create", SampleRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", field, message));
        return new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    private static class TestableCommonApplicationErrorAdvice extends CommonApplicationErrorAdvice {
        private ResponseEntity<Object> handleValidation(MethodArgumentNotValidException exception) {
            return handleMethodArgumentNotValid(
                    exception,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    new ServletWebRequest(new MockHttpServletRequest()));
        }
    }

    private static class SampleController {
        @SuppressWarnings("unused")
        void create(SampleRequest request) {
        }
    }

    private record SampleRequest() {
    }
}
