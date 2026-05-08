package com.batu.transaction_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.TransactionUpsertResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;
import com.batu.transaction_service.controller.TransactionInternalController;
import com.batu.transaction_service.config.KeycloakConfiguration;
import com.batu.transaction_service.service.TransactionService;

@WebMvcTest(
        controllers = TransactionInternalController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class TransactionInternalControllerTest {

    private static final UUID USER_ID = UUID.fromString("c4000000-0000-0000-0000-000000000001");
    private static final UUID TRANSACTION_ID = UUID.fromString("c4000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("c4000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void upsertTransaction_whenServiceJwtHasRole_shouldReturnUpsertResponse() throws Exception {
        when(transactionService.upsertTransaction(any(TransactionUpsertRequestDto.class))).thenReturn(upsertResponse(true));

        mockMvc.perform(post("/transactions/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.detailedCategoryCode").value("FOOD_AND_DRINK_COFFEE"))
                .andExpect(jsonPath("$.active").value(true));

        verify(transactionService).upsertTransaction(any(TransactionUpsertRequestDto.class));
    }

    @Test
    void upsertTransaction_whenJwtDoesNotHaveServiceRole_shouldReturnForbiddenAndNotCallService() throws Exception {
        mockMvc.perform(post("/transactions/internal/upsert")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson()))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).upsertTransaction(any(TransactionUpsertRequestDto.class));
    }

    @Test
    void upsertTransaction_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/transactions/internal/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson()))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).upsertTransaction(any(TransactionUpsertRequestDto.class));
    }

    @ParameterizedTest
    @CsvSource({
            "transactionId, transactionId",
            "userId, userId",
            "accountId, accountId",
            "amount, amount",
            "isoCurrencyCode, isoCurrencyCode",
            "transactionName, transactionName",
            "transactionType, transactionType",
            "date, date",
            "paymentChannel, paymentChannel",
            "detailedCategoryCode, detailedCategoryCode"
    })
    void upsertTransaction_whenRequiredFieldIsMissing_shouldReturnValidationProblemAndNotCallService(
            String omittedField,
            String errorField) throws Exception {
        mockMvc.perform(post("/transactions/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(upsertJsonWithout(omittedField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors." + errorField).exists());

        verify(transactionService, never()).upsertTransaction(any(TransactionUpsertRequestDto.class));
    }

    @ParameterizedTest
    @CsvSource({
            "US",
            "USDD"
    })
    void upsertTransaction_whenCurrencyLengthIsInvalid_shouldReturnValidationProblemAndNotCallService(String currency) throws Exception {
        mockMvc.perform(post("/transactions/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson().replace("\"USD\"", "\"" + currency + "\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.isoCurrencyCode").value("ISO currency code must be 3 characters"));

        verify(transactionService, never()).upsertTransaction(any(TransactionUpsertRequestDto.class));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor serviceJwt() {
        return jwt()
                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_service"));
    }

    private TransactionUpsertResponseDto upsertResponse(boolean active) {
        return new TransactionUpsertResponseDto(
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
                "FOOD_AND_DRINK_COFFEE",
                active,
                Instant.parse("2026-05-07T08:15:30Z"),
                Instant.parse("2026-05-07T09:15:30Z"));
    }

    private String validUpsertJson() {
        return """
                {
                  "transactionId": "c4000000-0000-0000-0000-000000000002",
                  "userId": "c4000000-0000-0000-0000-000000000001",
                  "accountId": "c4000000-0000-0000-0000-000000000003",
                  "amount": 42.50,
                  "isoCurrencyCode": "USD",
                  "transactionName": "Coffee Shop",
                  "transactionType": "place",
                  "date": "2026-05-07",
                  "pending": false,
                  "paymentChannel": "in store",
                  "detailedCategoryCode": "FOOD_AND_DRINK_COFFEE",
                  "active": true
                }
                """;
    }

    private String upsertJsonWithout(String fieldName) {
        return switch (fieldName) {
            case "transactionId" -> validUpsertJson().replace("  \"transactionId\": \"c4000000-0000-0000-0000-000000000002\",\n", "");
            case "userId" -> validUpsertJson().replace("  \"userId\": \"c4000000-0000-0000-0000-000000000001\",\n", "");
            case "accountId" -> validUpsertJson().replace("  \"accountId\": \"c4000000-0000-0000-0000-000000000003\",\n", "");
            case "amount" -> validUpsertJson().replace("  \"amount\": 42.50,\n", "");
            case "isoCurrencyCode" -> validUpsertJson().replace("  \"isoCurrencyCode\": \"USD\",\n", "");
            case "transactionName" -> validUpsertJson().replace("  \"transactionName\": \"Coffee Shop\",\n", "");
            case "transactionType" -> validUpsertJson().replace("  \"transactionType\": \"place\",\n", "");
            case "date" -> validUpsertJson().replace("  \"date\": \"2026-05-07\",\n", "");
            case "paymentChannel" -> validUpsertJson().replace("  \"paymentChannel\": \"in store\",\n", "");
            case "detailedCategoryCode" -> validUpsertJson().replace("  \"detailedCategoryCode\": \"FOOD_AND_DRINK_COFFEE\",\n", "");
            default -> throw new IllegalArgumentException("Unknown field " + fieldName);
        };
    }
}
