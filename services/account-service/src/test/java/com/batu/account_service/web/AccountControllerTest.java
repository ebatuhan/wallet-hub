package com.batu.account_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.batu.account_service.config.KeycloakConfiguration;
import com.batu.account_service.controller.AccountController;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = AccountController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class AccountControllerTest {

    private static final UUID USER_ID = UUID.fromString("c1000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("c1000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("c1000000-0000-0000-0000-000000000003");
    private static final Instant CREATED_AT = Instant.parse("2026-05-07T08:15:30Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-07T09:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAccount_whenAuthenticated_shouldReturnAccount() throws Exception {
        when(accountService.getAccount(eq(ACCOUNT_ID), any(Jwt.class))).thenReturn(accountResponse());

        mockMvc.perform(get("/accounts/{accountId}", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.institutionName").value("Bank"))
                .andExpect(jsonPath("$.accountName").value("Checking"))
                .andExpect(jsonPath("$.currentBalance").value(100.00))
                .andExpect(jsonPath("$.availableBalance").value(90.00))
                .andExpect(jsonPath("$.isoCurrencyCode").value("USD"));

        verify(accountService).getAccount(eq(ACCOUNT_ID), any(Jwt.class));
    }

    @Test
    void getAccount_whenAccountIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", "not-a-uuid")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccount(any(UUID.class), any(Jwt.class));
    }

    @Test
    void getAccount_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", ACCOUNT_ID))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).getAccount(any(UUID.class), any(Jwt.class));
    }

    @Test
    void getAccount_whenServiceThrowsNotFound_shouldReturnProblemDetail() throws Exception {
        when(accountService.getAccount(eq(ACCOUNT_ID), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        mockMvc.perform(get("/accounts/{accountId}", ACCOUNT_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void getAccountSummary_whenAuthenticated_shouldReturnCurrencyTotals() throws Exception {
        when(accountService.getAccountSummary(any(Jwt.class))).thenReturn(new AccountSummaryResponseDto(
                USER_ID,
                2L,
                List.of(new AccountCurrencyTotalDto("USD", new BigDecimal("150.00"), new BigDecimal("130.00")))));

        mockMvc.perform(get("/accounts/summary")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.activeAccountCount").value(2))
                .andExpect(jsonPath("$.totalsByCurrency[0].isoCurrencyCode").value("USD"))
                .andExpect(jsonPath("$.totalsByCurrency[0].currentBalanceTotal").value(150.00));
    }

    @Test
    void getAccountsByGivenIds_whenRequestIsValid_shouldReturnNames() throws Exception {
        when(accountService.getAccountsByGivenIds(any(AccountNameRequestDto.class))).thenReturn(List.of(
                new AccountNameResponseDto(ACCOUNT_ID, "Checking"),
                new AccountNameResponseDto(OTHER_ACCOUNT_ID, "Savings")));

        mockMvc.perform(post("/accounts/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "accountIds": [
                            "c1000000-0000-0000-0000-000000000002",
                            "c1000000-0000-0000-0000-000000000003"
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].accountName").value("Checking"))
                .andExpect(jsonPath("$[1].accountName").value("Savings"));

        verify(accountService).getAccountsByGivenIds(any(AccountNameRequestDto.class));
    }

    @Test
    void getAccountsByGivenIds_whenAccountIdsAreMissing_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(post("/accounts/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "accountIds": []
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.accountIds").value("At least one account id is required"));

        verify(accountService, never()).getAccountsByGivenIds(any(AccountNameRequestDto.class));
    }

    @Test
    void getAccounts_whenRequestIsValid_shouldReturnCursorResponse() throws Exception {
        CursorResponse<AccountViewDto> response = new CursorResponse<>(List.of(accountView()), true, "next-cursor");
        when(accountService.getAccountsViewPaginated(
                any(Jwt.class),
                eq("check"),
                eq("bank"),
                eq("depository"),
                eq("checking"),
                eq("cursor-1"),
                eq(20),
                eq(AccountSortField.CURRENT_BALANCE),
                eq(Sort.Direction.ASC))).thenReturn(response);

        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("accountName", "check")
                .param("institutionName", "bank")
                .param("accountType", "depository")
                .param("accountSubtype", "checking")
                .param("cursor", "cursor-1")
                .param("limit", "20")
                .param("sortBy", "CURRENT_BALANCE")
                .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.data[0].accountName").value("Checking"))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"));

        verify(accountService).getAccountsViewPaginated(
                any(Jwt.class),
                eq("check"),
                eq("bank"),
                eq("depository"),
                eq("checking"),
                eq("cursor-1"),
                eq(20),
                eq(AccountSortField.CURRENT_BALANCE),
                eq(Sort.Direction.ASC));
    }

    @ParameterizedTest
    @CsvSource({
            "0, Limit must be at least 1",
            "101, Limit cannot exceed 100"
    })
    void getAccounts_whenLimitIsOutsideAllowedBoundary_shouldReturnValidationProblemAndNotCallService(
            String limit,
            String expectedMessage) throws Exception {
        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value(expectedMessage));

        verify(accountService, never()).getAccountsViewPaginated(any(), any(), any(), any(), any(), any(), any(Integer.class), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "100" })
    void getAccounts_whenLimitIsOnAllowedBoundary_shouldReturnCursorResponse(String limit) throws Exception {
        when(accountService.getAccountsViewPaginated(any(), any(), any(), any(), any(), any(), eq(Integer.parseInt(limit)), any(), any()))
                .thenReturn(new CursorResponse<>(List.of(accountView()), false, null));

        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(accountService).getAccountsViewPaginated(any(), any(), any(), any(), any(), any(), eq(Integer.parseInt(limit)), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "UNKNOWN", "current_balance" })
    void getAccounts_whenSortByIsInvalid_shouldReturnBadRequestAndNotCallService(String sortBy) throws Exception {
        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("sortBy", sortBy))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountsViewPaginated(any(), any(), any(), any(), any(), any(), any(Integer.class), any(), any());
    }

    @Test
    void getAccounts_whenDirectionIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("direction", "SIDEWAYS"))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).getAccountsViewPaginated(any(), any(), any(), any(), any(), any(), any(Integer.class), any(), any());
    }

    @Test
    void getAccounts_whenCursorIsMalformed_shouldReturnProblemDetail() throws Exception {
        when(accountService.getAccountsViewPaginated(any(), any(), any(), any(), any(), eq("malformed"), any(Integer.class), any(), any()))
                .thenThrow(new IllegalArgumentException("Malformed cursor"));

        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("cursor", "malformed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed cursor"));
    }

    private AccountResponseDto accountResponse() {
        return new AccountResponseDto(
                ACCOUNT_ID,
                "Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                CREATED_AT,
                UPDATED_AT);
    }

    private AccountViewDto accountView() {
        return new AccountViewDto(
                ACCOUNT_ID,
                "Bank",
                "Checking",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                "depository",
                "checking",
                "1234",
                CREATED_AT,
                UPDATED_AT);
    }
}
