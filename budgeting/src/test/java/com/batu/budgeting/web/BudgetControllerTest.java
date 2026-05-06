package com.batu.budgeting.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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

import com.batu.budgeting.config.KeycloakConfiguration;
import com.batu.budgeting.controller.BudgetController;
import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.BudgetPeriod;
import com.batu.budgeting.enums.BudgetSortField;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = BudgetController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class BudgetControllerTest {

    private static final UUID USER_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID BUDGET_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("60000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createBudget_whenRequestIsValid_shouldReturnBudgetResponse() throws Exception {
        when(budgetService.createBudget(any(CreateBudgetRequest.class), any(Jwt.class)))
                .thenReturn(response());

        mockMvc.perform(post("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.limitAmount").value(500.00))
                .andExpect(jsonPath("$.spentAmount").value(125.25))
                .andExpect(jsonPath("$.isoCurrencyCode").value("USD"))
                .andExpect(jsonPath("$.period").value("MONTHLY"))
                .andExpect(jsonPath("$.periodStart").value("2026-05-01"))
                .andExpect(jsonPath("$.periodEnd").value("2026-05-31"))
                .andExpect(jsonPath("$.active").value(true));

        verify(budgetService).createBudget(any(CreateBudgetRequest.class), any(Jwt.class));
    }

    @Test
    void createBudget_whenRequestIsInvalid_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(post("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": null,
                          "limitAmount": 0,
                          "isoCurrencyCode": "US",
                          "period": null,
                          "periodStart": null
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.categoryId").exists())
                .andExpect(jsonPath("$.errors.limitAmount").exists())
                .andExpect(jsonPath("$.errors.isoCurrencyCode").exists())
                .andExpect(jsonPath("$.errors.period").exists())
                .andExpect(jsonPath("$.errors.periodStart").exists());

        verify(budgetService, never()).createBudget(any(CreateBudgetRequest.class), any(Jwt.class));
    }

    @Test
    void createBudget_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isUnauthorized());

        verify(budgetService, never()).createBudget(any(CreateBudgetRequest.class), any(Jwt.class));
    }

    @Test
    void createBudget_whenServiceThrowsConflict_shouldReturnProblemDetail() throws Exception {
        when(budgetService.createBudget(any(CreateBudgetRequest.class), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Overlapping active budget exists for this category and currency"));

        mockMvc.perform(post("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Overlapping active budget exists for this category and currency"))
                .andExpect(jsonPath("$.message").value("Overlapping active budget exists for this category and currency"));
    }

    @Test
    void updateBudget_whenRequestIsValid_shouldReturnBudgetResponse() throws Exception {
        when(budgetService.updateBudget(eq(BUDGET_ID), any(CreateBudgetRequest.class), any(Jwt.class)))
                .thenReturn(response());

        mockMvc.perform(put("/budgets/{budgetId}", BUDGET_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()));

        verify(budgetService).updateBudget(eq(BUDGET_ID), any(CreateBudgetRequest.class), any(Jwt.class));
    }

    @Test
    void updateBudget_whenBudgetIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(put("/budgets/{budgetId}", "not-a-uuid")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).updateBudget(any(UUID.class), any(CreateBudgetRequest.class), any(Jwt.class));
    }

    @Test
    void updateBudget_whenServiceThrowsNotFound_shouldReturnProblemDetail() throws Exception {
        when(budgetService.updateBudget(eq(BUDGET_ID), any(CreateBudgetRequest.class), any(Jwt.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget with id " + BUDGET_ID + " not found"));

        mockMvc.perform(put("/budgets/{budgetId}", BUDGET_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Budget with id " + BUDGET_ID + " not found"));
    }

    @Test
    void getBudgets_whenRequestIsValid_shouldReturnCursorResponse() throws Exception {
        CursorResponse<BudgetResponse> cursorResponse = new CursorResponse<>(List.of(response()), true, "next-cursor");
        when(budgetService.getBudgets(any(Jwt.class), eq("cursor-1"), eq(20), eq(BudgetSortField.LIMIT_AMOUNT), eq(Sort.Direction.ASC)))
                .thenReturn(cursorResponse);

        mockMvc.perform(get("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("cursor", "cursor-1")
                .param("limit", "20")
                .param("sortBy", "LIMIT_AMOUNT")
                .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(BUDGET_ID.toString()))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"));

        verify(budgetService).getBudgets(any(Jwt.class), eq("cursor-1"), eq(20), eq(BudgetSortField.LIMIT_AMOUNT), eq(Sort.Direction.ASC));
    }

    @Test
    void getBudgets_whenLimitIsBelowMinimum_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(get("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value("Limit must be at least 1"));

        verify(budgetService, never()).getBudgets(any(Jwt.class), any(), any(Integer.class), any(), any());
    }

    @Test
    void getBudgets_whenLimitExceedsMaximum_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(get("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.limit").value("Limit cannot exceed 100"));

        verify(budgetService, never()).getBudgets(any(Jwt.class), any(), any(Integer.class), any(), any());
    }

    @Test
    void getBudgets_whenSortByIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/budgets")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .param("sortBy", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).getBudgets(any(Jwt.class), any(), any(Integer.class), any(), any());
    }

    @Test
    void deactivateBudget_whenBudgetIdIsValid_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/budgets/{budgetId}", BUDGET_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(budgetService).deactivateBudget(eq(BUDGET_ID), any(Jwt.class));
    }

    @Test
    void deactivateBudget_whenBudgetIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(delete("/budgets/{budgetId}", "not-a-uuid")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).deactivateBudget(any(UUID.class), any(Jwt.class));
    }

    private BudgetResponse response() {
        return new BudgetResponse(
                BUDGET_ID,
                CATEGORY_ID,
                new BigDecimal("500.00"),
                new BigDecimal("125.25"),
                "USD",
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true);
    }

    private String validRequestJson() {
        return """
                {
                  "categoryId": "60000000-0000-0000-0000-000000000003",
                  "limitAmount": 500.00,
                  "isoCurrencyCode": "USD",
                  "period": "MONTHLY",
                  "periodStart": "2026-05-01"
                }
                """;
    }
}
