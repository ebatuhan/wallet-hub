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

import com.batu.insights_service.config.KeycloakConfiguration;
import com.batu.insights_service.controller.AccountInsightController;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = AccountInsightController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class AccountInsightControllerTest {

    private static final UUID USER_ID = UUID.fromString("8c000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("8c000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountInsightsService accountInsightsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAccountBalanceHistory_whenAuthenticated_shouldReturnChronologicalBalancePointsAndPassJwt() throws Exception {
        ArgumentCaptor<Jwt> jwtCaptor = ArgumentCaptor.forClass(Jwt.class);
        when(accountInsightsService.getAccountBalanceHistory(
                eq(ACCOUNT_ID),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                any(Jwt.class)))
                .thenReturn(List.of(
                        point("2026-04-01", "100.00"),
                        point("2026-04-02", "125.50")));

        mockMvc.perform(get("/api/insights/accounts/{accountId}/balance-history", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04-01")
                .param("to", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].balance").value(100.00))
                .andExpect(jsonPath("$[0].isoCurrencyCode").value("USD"))
                .andExpect(jsonPath("$[0].date").value("2026-04-01"))
                .andExpect(jsonPath("$[1].balance").value(125.50))
                .andExpect(jsonPath("$[1].date").value("2026-04-02"));

        verify(accountInsightsService).getAccountBalanceHistory(
                eq(ACCOUNT_ID),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                jwtCaptor.capture());
        assertThat(jwtCaptor.getValue().getSubject()).isEqualTo(USER_ID.toString());
    }

    @Test
    void getAccountBalanceHistory_whenServiceReturnsEmptyList_shouldReturnEmptyJsonArray() throws Exception {
        when(accountInsightsService.getAccountBalanceHistory(
                eq(ACCOUNT_ID),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                any(Jwt.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/insights/accounts/{accountId}/balance-history", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04-01")
                .param("to", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAccountBalanceHistory_whenJwtMissing_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/insights/accounts/{accountId}/balance-history", ACCOUNT_ID)
                .param("from", "2026-04-01")
                .param("to", "2026-04-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccountBalanceHistory_whenAccountIdMalformed_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/api/insights/accounts/not-a-uuid/balance-history")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", "2026-04-01")
                .param("to", "2026-04-30"))
                .andExpect(status().isBadRequest());

        verify(accountInsightsService, never()).getAccountBalanceHistory(any(UUID.class), any(), any(), any(Jwt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "from", "to" })
    void getAccountBalanceHistory_whenDateFormatInvalid_shouldReturnBadRequestAndNotCallService(String invalidParam)
            throws Exception {
        String from = invalidParam.equals("from") ? "not-a-date" : "2026-04-01";
        String to = invalidParam.equals("to") ? "not-a-date" : "2026-04-30";

        mockMvc.perform(get("/api/insights/accounts/{accountId}/balance-history", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("from", from)
                .param("to", to))
                .andExpect(status().isBadRequest());

        verify(accountInsightsService, never()).getAccountBalanceHistory(any(UUID.class), any(), any(), any(Jwt.class));
    }

    private static AccountBalanceDataPointDto point(String date, String balance) {
        return new AccountBalanceDataPointDto(
                ACCOUNT_ID,
                USER_ID,
                new BigDecimal(balance),
                "USD",
                LocalDate.parse(date));
    }
}
