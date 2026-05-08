package com.batu.account_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

import com.batu.account_service.config.KeycloakConfiguration;
import com.batu.account_service.controller.AccountInternalController;
import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = AccountInternalController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class AccountInternalControllerTest {

    private static final UUID USER_ID = UUID.fromString("c2000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("c2000000-0000-0000-0000-000000000002");
    private static final UUID CONNECTION_ID = UUID.fromString("c2000000-0000-0000-0000-000000000003");
    private static final Instant CREATED_AT = Instant.parse("2026-05-07T08:15:30Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-07T09:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void findAccountsByConnectionId_whenServiceJwtHasRole_shouldReturnAccounts() throws Exception {
        when(accountService.findAccountsByConnectionId(CONNECTION_ID)).thenReturn(List.of(accountResponse()));

        mockMvc.perform(get("/accounts/internal/by-connection/{connectionId}", CONNECTION_ID)
                .with(serviceJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].accountName").value("Checking"));

        verify(accountService).findAccountsByConnectionId(CONNECTION_ID);
    }

    @Test
    void findAccountsByConnectionId_whenConnectionIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/accounts/internal/by-connection/{connectionId}", "not-a-uuid")
                .with(serviceJwt()))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).findAccountsByConnectionId(any(UUID.class));
    }

    @Test
    void findAccountsByConnectionId_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts/internal/by-connection/{connectionId}", CONNECTION_ID))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).findAccountsByConnectionId(any(UUID.class));
    }

    @Test
    void findAccountsByConnectionId_whenJwtDoesNotHaveServiceRole_shouldReturnForbiddenAndNotCallService() throws Exception {
        mockMvc.perform(get("/accounts/internal/by-connection/{connectionId}", CONNECTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden());

        verify(accountService, never()).findAccountsByConnectionId(any(UUID.class));
    }

    @Test
    void upsertAccount_whenRequestIsValidAndServiceJwtHasRole_shouldReturnUpsertResponse() throws Exception {
        when(accountService.upsertAccount(any(AccountUpsertRequestDto.class))).thenReturn(upsertResponse(true));

        mockMvc.perform(post("/accounts/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.connectionId").value(CONNECTION_ID.toString()))
                .andExpect(jsonPath("$.accountName").value("Checking"))
                .andExpect(jsonPath("$.active").value(true));

        verify(accountService).upsertAccount(any(AccountUpsertRequestDto.class));
    }

    @ParameterizedTest
    @CsvSource({
            "accountId, accountId",
            "userId, userId",
            "connectionId, connectionId",
            "institutionName, institutionName",
            "accountName, accountName",
            "accountType, accountType",
            "accountMask, accountMask",
            "currentBalance, currentBalance",
            "availableBalance, availableBalance",
            "isoCurrencyCode, isoCurrencyCode"
    })
    void upsertAccount_whenRequiredFieldIsMissing_shouldReturnValidationProblemAndNotCallService(
            String omittedField,
            String errorField) throws Exception {
        mockMvc.perform(post("/accounts/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(upsertJsonWithout(omittedField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors." + errorField).exists());

        verify(accountService, never()).upsertAccount(any(AccountUpsertRequestDto.class));
    }

    @Test
    void upsertAccount_whenCurrencyLengthIsInvalid_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(post("/accounts/internal/upsert")
                .with(serviceJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpsertJson().replace("\"USD\"", "\"US\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.isoCurrencyCode").value("ISO currency code must be 3 characters"));

        verify(accountService, never()).upsertAccount(any(AccountUpsertRequestDto.class));
    }

    @Test
    void deactivateAccountsByConnection_whenServiceJwtHasRole_shouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/accounts/internal/deactivate-by-connection/{connectionId}", CONNECTION_ID)
                .with(serviceJwt()))
                .andExpect(status().isNoContent());

        verify(accountService).deactivateAccountsByConnection(CONNECTION_ID);
    }

    @Test
    void deactivateAccountsByConnection_whenConnectionIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(put("/accounts/internal/deactivate-by-connection/{connectionId}", "not-a-uuid")
                .with(serviceJwt()))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).deactivateAccountsByConnection(any(UUID.class));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor serviceJwt() {
        return jwt()
                .jwt(jwt -> jwt.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_service"));
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

    private AccountUpsertResponseDto upsertResponse(boolean active) {
        return new AccountUpsertResponseDto(
                ACCOUNT_ID,
                USER_ID,
                CONNECTION_ID,
                "Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                active,
                CREATED_AT,
                UPDATED_AT);
    }

    private String validUpsertJson() {
        return """
                {
                  "accountId": "c2000000-0000-0000-0000-000000000002",
                  "userId": "c2000000-0000-0000-0000-000000000001",
                  "connectionId": "c2000000-0000-0000-0000-000000000003",
                  "institutionName": "Bank",
                  "accountName": "Checking",
                  "accountType": "depository",
                  "accountSubtype": "checking",
                  "accountMask": "1234",
                  "currentBalance": 100.00,
                  "availableBalance": 90.00,
                  "isoCurrencyCode": "USD"
                }
                """;
    }

    private String upsertJsonWithout(String fieldName) {
        return switch (fieldName) {
            case "accountId" -> validUpsertJson().replace("\"accountId\": \"c2000000-0000-0000-0000-000000000002\",\n", "");
            case "userId" -> validUpsertJson().replace("\"userId\": \"c2000000-0000-0000-0000-000000000001\",\n", "");
            case "connectionId" -> validUpsertJson().replace("\"connectionId\": \"c2000000-0000-0000-0000-000000000003\",\n", "");
            case "institutionName" -> validUpsertJson().replace("\"institutionName\": \"Bank\",\n", "");
            case "accountName" -> validUpsertJson().replace("\"accountName\": \"Checking\",\n", "");
            case "accountType" -> validUpsertJson().replace("\"accountType\": \"depository\",\n", "");
            case "accountMask" -> validUpsertJson().replace("\"accountMask\": \"1234\",\n", "");
            case "currentBalance" -> validUpsertJson().replace("\"currentBalance\": 100.00,\n", "");
            case "availableBalance" -> validUpsertJson().replace("\"availableBalance\": 90.00,\n", "");
            case "isoCurrencyCode" -> validUpsertJson().replace("  \"availableBalance\": 90.00,\n  \"isoCurrencyCode\": \"USD\"\n", "  \"availableBalance\": 90.00\n");
            default -> throw new IllegalArgumentException("Unknown field " + fieldName);
        };
    }
}
