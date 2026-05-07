package com.batu.plaid_adapter_service.unit.strategy.plaiderror;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.strategy.impl.plaid_error.DefaultPlaidErrorHandlerStrategy;
import com.batu.plaid_adapter_service.strategy.impl.plaid_error.RateLimitErrorHandlerStrategy;
import com.batu.plaid_adapter_service.strategy.impl.plaid_error.TransactionsMutationHandlerStrategy;
import com.plaid.client.model.PlaidError;

class PlaidErrorHandlerStrategyTest {

    @Test
    void defaultStrategy_whenDisplayMessageExists_shouldReturnBadGatewayWithDisplayMessage() {
        RuntimeException exception = new DefaultPlaidErrorHandlerStrategy()
                .toException(new PlaidError().displayMessage("Try again later").errorMessage("raw message"));

        assertThat(exception).isInstanceOfSatisfying(ResponseStatusException.class, responseException -> {
            assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(responseException.getReason()).isEqualTo("Try again later");
        });
    }

    @Test
    void defaultStrategy_whenDisplayMessageMissing_shouldReturnGenericBadGateway() {
        RuntimeException exception = new DefaultPlaidErrorHandlerStrategy().toException(new PlaidError());

        assertThat(exception).isInstanceOfSatisfying(ResponseStatusException.class, responseException -> {
            assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(responseException.getReason()).isEqualTo("An error occurred in Plaid");
        });
    }

    @Test
    void rateLimitStrategy_whenErrorReceived_shouldReturnRetryableTooManyRequests() {
        RuntimeException exception = new RateLimitErrorHandlerStrategy().toException(new PlaidError());

        assertThat(exception).isInstanceOfSatisfying(PlaidRetryableException.class, retryableException -> {
            assertThat(retryableException.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(retryableException.getReason()).isEqualTo("Service exceed rate limit on calling Plaid API.");
        });
    }

    @Test
    void transactionsMutationStrategy_whenErrorReceived_shouldReturnRetryableBadGatewayWithPlaidMessage() {
        RuntimeException exception = new TransactionsMutationHandlerStrategy()
                .toException(new PlaidError().errorMessage("sync mutated"));

        assertThat(exception).isInstanceOfSatisfying(PlaidRetryableException.class, retryableException -> {
            assertThat(retryableException.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(retryableException.getReason()).isEqualTo("sync mutated");
        });
    }
}
