package com.batu.dashboard_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.batu.dashboard_service.config.KeycloakConfiguration;
import com.batu.dashboard_service.controller.DashboardController;
import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;
import com.batu.dashboard_service.service.DashboardService;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = DashboardController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class DashboardControllerTest {

    private static final UUID USER_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("71000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @ParameterizedTest
    @CsvSource({
            "0, Limit must be at least 1",
            "101, Limit cannot exceed 100"
    })
    void getSummary_whenRecentLimitIsOutsideAllowedBoundary_shouldReturnValidationProblemAndNotCallService(
            String limit,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/dashboard/summary")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("recentLimit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.recentLimit").value(expectedMessage));

        verify(dashboardService, never()).getUserSummary(any(), any(), any(), any(Jwt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "100" })
    void getSummary_whenRecentLimitIsOnAllowedBoundary_shouldCallService(String limit) throws Exception {
        when(dashboardService.getUserSummary(any(), any(), eq(Integer.parseInt(limit)), any(Jwt.class)))
                .thenReturn(userSummary());

        mockMvc.perform(get("/dashboard/summary")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("recentLimit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));

        verify(dashboardService).getUserSummary(any(), any(), eq(Integer.parseInt(limit)), any(Jwt.class));
    }

    @ParameterizedTest
    @CsvSource({
            "0, Limit must be at least 1",
            "101, Limit cannot exceed 100"
    })
    void getBudgets_whenLimitIsOutsideAllowedBoundary_shouldReturnValidationProblemAndNotCallService(
            String limit,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/dashboard/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value(expectedMessage));

        verify(dashboardService, never()).getBudgets(any(), any(), any(), any(), any(Jwt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "100" })
    void getBudgets_whenLimitIsOnAllowedBoundary_shouldCallService(String limit) throws Exception {
        when(dashboardService.getBudgets(eq(Integer.parseInt(limit)), any(), any(), any(), any(Jwt.class)))
                .thenReturn(new CursorResponse<BudgetResponseDto>(List.of(), false, null));

        mockMvc.perform(get("/dashboard/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(dashboardService).getBudgets(eq(Integer.parseInt(limit)), any(), any(), any(), any(Jwt.class));
    }

    @ParameterizedTest
    @CsvSource({
            "0, Limit must be at least 1",
            "101, Limit cannot exceed 100"
    })
    void getAccountSummary_whenLimitIsOutsideAllowedBoundary_shouldReturnValidationProblemAndNotCallService(
            String limit,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/dashboard/accounts/{accountId}/summary", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value(expectedMessage));

        verify(dashboardService, never()).getAccountSummary(any(UUID.class), any(), any(), any(), any(), any(Jwt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "100" })
    void getAccountSummary_whenLimitIsOnAllowedBoundary_shouldCallService(String limit) throws Exception {
        when(dashboardService.getAccountSummary(eq(ACCOUNT_ID), any(), any(), eq(Integer.parseInt(limit)), any(), any(Jwt.class)))
                .thenReturn(accountSummary());

        mockMvc.perform(get("/dashboard/accounts/{accountId}/summary", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));

        verify(dashboardService).getAccountSummary(eq(ACCOUNT_ID), any(), any(), eq(Integer.parseInt(limit)), any(), any(Jwt.class));
    }

    private UserDashboardSummaryResponseDto userSummary() {
        return new UserDashboardSummaryResponseDto(USER_ID, null, null, null, null, null);
    }

    private AccountDashboardSummaryResponseDto accountSummary() {
        return new AccountDashboardSummaryResponseDto(USER_ID, null, null, List.of(), null, null);
    }
}
