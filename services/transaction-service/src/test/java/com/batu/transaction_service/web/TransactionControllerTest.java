package com.batu.transaction_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDetailedCategoryDto;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;
import com.batu.transaction_service.controller.TransactionController;
import com.batu.transaction_service.config.KeycloakConfiguration;
import com.batu.transaction_service.service.TransactionService;

@WebMvcTest(
        controllers = TransactionController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class TransactionControllerTest {

    private static final UUID USER_ID = UUID.fromString("c3000000-0000-0000-0000-000000000001");
    private static final UUID TRANSACTION_ID = UUID.fromString("c3000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("c3000000-0000-0000-0000-000000000003");
    private static final UUID PRIMARY_CATEGORY_ID = UUID.fromString("c3000000-0000-0000-0000-000000000004");
    private static final UUID DETAILED_CATEGORY_ID = UUID.fromString("c3000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getTransactions_whenAuthenticatedAndQueryParamsProvided_shouldReturnCursorResponse() throws Exception {
        when(transactionService.transactions(any(Jwt.class), eq("FOOD_AND_DRINK"), eq(ACCOUNT_ID), eq("cursor-1"), eq(20)))
                .thenReturn(new CursorResponse<>(List.of(transactionView()), true, "cursor-2"));

        mockMvc.perform(get("/transactions")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("category", "FOOD_AND_DRINK")
                .param("accountId", ACCOUNT_ID.toString())
                .param("cursor", "cursor-1")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.data[0].transactionName").value("Coffee Shop"))
                .andExpect(jsonPath("$.data[0].primaryCategoryCode").value("FOOD_AND_DRINK"))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"));

        verify(transactionService).transactions(any(Jwt.class), eq("FOOD_AND_DRINK"), eq(ACCOUNT_ID), eq("cursor-1"), eq(20));
    }

    @ParameterizedTest
    @CsvSource({
            "0, Limit must be at least 1",
            "101, Limit cannot exceed 100"
    })
    void getTransactions_whenLimitIsOutsideAllowedBoundary_shouldReturnValidationProblemAndNotCallService(
            String limit,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/transactions")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value(expectedMessage));

        verify(transactionService, never()).transactions(any(Jwt.class), any(), any(), any(), any(Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "100" })
    void getTransactions_whenLimitBoundaryIsProvided_shouldPassLimitToService(String limit) throws Exception {
        when(transactionService.transactions(any(Jwt.class), any(), any(), any(), eq(Integer.parseInt(limit))))
                .thenReturn(new CursorResponse<>(List.of(transactionView()), false, null));

        mockMvc.perform(get("/transactions")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(transactionService).transactions(any(Jwt.class), any(), any(), any(), eq(Integer.parseInt(limit)));
    }

    @Test
    void getTransactions_whenAccountIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/transactions")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("accountId", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transactions(any(Jwt.class), any(), any(), any(), any(Integer.class));
    }

    @Test
    void getTransactions_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).transactions(any(Jwt.class), any(), any(), any(), any(Integer.class));
    }

    @Test
    void getTransactionById_whenAuthenticated_shouldReturnTransaction() throws Exception {
        when(transactionService.getTransactionById(any(Jwt.class), eq(TRANSACTION_ID))).thenReturn(transactionDto());

        mockMvc.perform(get("/transactions/{transactionId}", TRANSACTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.detailedCategory.detailedCode").value("FOOD_AND_DRINK_COFFEE"))
                .andExpect(jsonPath("$.detailedCategory.primaryCategory.categoryCode").value("FOOD_AND_DRINK"));

        verify(transactionService).getTransactionById(any(Jwt.class), eq(TRANSACTION_ID));
    }

    @Test
    void getTransactionById_whenTransactionIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/transactions/{transactionId}", "not-a-uuid")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).getTransactionById(any(Jwt.class), any(UUID.class));
    }

    @Test
    void getTransactionById_whenServiceThrowsNotFound_shouldReturnProblemDetail() throws Exception {
        when(transactionService.getTransactionById(any(Jwt.class), eq(TRANSACTION_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        mockMvc.perform(get("/transactions/{transactionId}", TRANSACTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transaction not found"))
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    private TransactionViewResponseDto transactionView() {
        return new TransactionViewResponseDto(
                TRANSACTION_ID,
                new BigDecimal("42.50"),
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                "USD",
                PRIMARY_CATEGORY_ID,
                "FOOD_AND_DRINK",
                "Food & Drink",
                "food.svg",
                DETAILED_CATEGORY_ID,
                "FOOD_AND_DRINK_COFFEE",
                "Coffee",
                ACCOUNT_ID);
    }

    private TransactionDto transactionDto() {
        return new TransactionDto(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("42.50"),
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                new TransactionDetailedCategoryDto(
                        DETAILED_CATEGORY_ID,
                        "Coffee",
                        "FOOD_AND_DRINK_COFFEE",
                        new TransactionPrimaryCategoryDto(PRIMARY_CATEGORY_ID, "FOOD_AND_DRINK", "Food & Drink", "food.svg")),
                Instant.parse("2026-05-07T08:15:30Z"),
                Instant.parse("2026-05-07T09:15:30Z"));
    }
}
