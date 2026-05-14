package com.batu.insights_service.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.batu.insights_service.config.KeycloakConfiguration;
import com.batu.insights_service.controller.TransactionInsightsController;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;
import com.batu.shared.dto.response.SpendingCurrencyGroupDto;
import com.batu.shared.dto.response.SpendingGraphPointDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingGraphSeriesDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = TransactionInsightsController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class TransactionInsightsControllerTest {

    private static final UUID USER_ID = UUID.fromString("8b000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("8b000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("8b000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionInsightsService transactionInsightsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getSpendingByCategory_whenAuthenticated_shouldPassPeriodAndJwt() throws Exception {
        ArgumentCaptor<Jwt> jwtCaptor = ArgumentCaptor.forClass(Jwt.class);
        when(transactionInsightsService.getSpendingByCategory(eq("2026-04"), any(Jwt.class)))
                .thenReturn(new SpendingPerCategoryResponseDto(USER_ID, List.of(currencyGroup())));

        mockMvc.perform(get("/api/insights/spendings")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.spendingByCurrency[0].isoCurrencyCode").value("USD"));

        verify(transactionInsightsService).getSpendingByCategory(eq("2026-04"), jwtCaptor.capture());
        assertThat(jwtCaptor.getValue().getSubject()).isEqualTo(USER_ID.toString());
    }

    @Test
    void getSpendingGraph_whenAuthenticated_shouldReturnGraphSeriesJson() throws Exception {
        when(transactionInsightsService.getSpendingGraph(eq("2026"), any(Jwt.class)))
                .thenReturn(new SpendingGraphResponseDto(
                        USER_ID,
                        null,
                        "MONTH",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        List.of(new SpendingGraphSeriesDto("USD", List.of(
                                new SpendingGraphPointDto(LocalDate.of(2026, 1, 1), decimal("25.50")))))));

        mockMvc.perform(get("/api/insights/spendings/graph")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("MONTH"))
                .andExpect(jsonPath("$.from").value("2026-01-01"));
    }

    @Test
    void getIncome_whenAuthenticated_shouldReturnIncomeTotals() throws Exception {
        when(transactionInsightsService.getIncome(eq(date("2026-04-01")), eq(date("2026-04-30")), any(Jwt.class)))
                .thenReturn(new IncomeSummaryResponseDto(USER_ID, List.of(new IncomeTotalByCurrencyDto("USD", decimal("1000.00")))));

        mockMvc.perform(get("/api/insights/income")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04-01")
                .param("to", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalsByCurrency[0].totalIncome").value(1000.00));
    }

    @Test
    void getSpendingByCategoryByAccount_whenAuthenticated_shouldReturnAccountScopedSpending() throws Exception {
        when(transactionInsightsService.getSpendingPerCategoryByAccount(eq("2026-04"), eq(ACCOUNT_ID), any(Jwt.class)))
                .thenReturn(new SpendingPerCategoryByAccountResponseDto(USER_ID, ACCOUNT_ID, List.of(currencyGroup())));

        mockMvc.perform(get("/api/insights/spendings/{accountId}", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()));
    }

    @Test
    void getSpendingGraphByAccount_whenAuthenticated_shouldReturnAccountScopedGraph() throws Exception {
        when(transactionInsightsService.getSpendingGraphByAccount(eq("2026-04"), eq(ACCOUNT_ID), any(Jwt.class)))
                .thenReturn(new SpendingGraphResponseDto(
                        USER_ID,
                        ACCOUNT_ID,
                        "WEEK",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30),
                        List.of(new SpendingGraphSeriesDto("USD", List.of(
                                new SpendingGraphPointDto(LocalDate.of(2026, 3, 30), decimal("42.50")))))));

        mockMvc.perform(get("/api/insights/spendings/graph/{accountId}", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.groupBy").value("WEEK"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/insights/spendings",
            "/api/insights/spendings/graph",
            "/api/insights/income"
    })
    void globalEndpoints_whenJwtMissing_shouldReturnUnauthorized(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint).param("from", "2026-04"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/insights/spendings/not-a-uuid",
            "/api/insights/spendings/graph/not-a-uuid"
    })
    void accountEndpoints_whenAccountIdMalformed_shouldReturnBadRequestAndNotCallService(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04"))
                .andExpect(status().isBadRequest());

        verify(transactionInsightsService, never()).getSpendingPerCategoryByAccount(any(), any(UUID.class), any(Jwt.class));
        verify(transactionInsightsService, never()).getSpendingGraphByAccount(any(), any(UUID.class), any(Jwt.class));
    }

    @Test
    void getSpendingByCategory_whenServiceRejectsPeriod_shouldReturnProblemDetailBadRequest() throws Exception {
        when(transactionInsightsService.getSpendingByCategory(eq("bad"), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "'from' must be YYYY or YYYY-MM"));

        mockMvc.perform(get("/api/insights/spendings")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private static SpendingCurrencyGroupDto currencyGroup() {
        return new SpendingCurrencyGroupDto(
                "USD",
                decimal("42.50"),
                List.of(new SpendingPerCategoryDto(CATEGORY_ID, decimal("100.00"), decimal("42.50"))));
    }

    private static Date date(String value) {
        return Date.valueOf(value);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
