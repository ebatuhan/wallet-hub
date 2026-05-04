package com.batu.ai_assistant.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.client.BudgetingClient;
import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;
import com.batu.shared.dto.response.BudgetResponseDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BudgetTools {

    private final BudgetingClient budgetingClient;
    private final DashboardClient dashboardClient;

    @Tool(
        name = "get_budgets",
        description = """
            Returns all active budgets for the user. \
            Call this proactively whenever the user asks about budgets or before suggesting new ones \
            to avoid duplicating an already existing budget."""
    )
    public ToolResponse<java.util.List<BudgetResponseDto>> getBudgets(ToolContext toolContext) {
        String authorization = authorization(toolContext);
        if (authorization == null) {
            return ToolResponse.failure("Cannot read budgets because the caller authorization is unavailable.");
        }
        return ToolResponse.success("Active enriched budgets loaded.", dashboardClient.getBudgets(authorization).getBody());
    }

    @Tool(
        name = "create_budget",
        description = """
            Creates a new budget for a category. \
            Always resolve the categoryId silently using get_all_primary_categories before calling this — never expose this step to the user. \
            If period is omitted, default to MONTHLY. \
            If periodStart is omitted, default to the first day of the current month. \
            Only call this after the user has explicitly confirmed they want the budget created."""
    )
    public ToolResponse<BudgetResponseDto> createBudget(
            @ToolParam(description = "Internal category ID resolved from get_all_primary_categories. Never ask the user for this.") String categoryId,
            @ToolParam(description = "The budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart,
            ToolContext toolContext) {
        String authorization = authorization(toolContext);
        if (authorization == null) {
            return ToolResponse.failure("Cannot create the budget because the caller authorization is unavailable.");
        }
        UUID resolvedCategoryId = parseUuid(categoryId);
        if (resolvedCategoryId == null) {
            return ToolResponse.failure("The categoryId is invalid. Call get_all_primary_categories and use an exact returned category ID.");
        }
        LocalDate resolvedPeriodStart = parsePeriodStart(periodStart);
        if (resolvedPeriodStart == null) {
            return ToolResponse.failure("The periodStart must use yyyy-MM-dd format, or be omitted for the current month.");
        }
        BudgetResponseDto budget = budgetingClient.createBudget(
                authorization,
                new CreateBudgetRequestDto(
                        resolvedCategoryId,
                        limitAmount,
                        isoCurrencyCode,
                        normalizePeriod(period),
                        resolvedPeriodStart))
                .getBody();
        return ToolResponse.success("Budget created.", budget);
    }

    @Tool(
        name = "update_budget",
        description = """
            Updates an existing budget by its ID. \
            Always resolve the categoryId silently using get_all_primary_categories and  call get_budgets tool before calling this to see current budgets of user, use this when user asks or you need to update the budget of the user. — never expose this step to the user. \
            If period is omitted, default to MONTHLY. \
            If periodStart is omitted, default to the first day of the current month. \
            Only call this after the user has explicitly confirmed they want the change applied."""
    )
    public ToolResponse<BudgetResponseDto> updateBudget(
            @ToolParam(description = "The ID of the budget to update.") String budgetId,
            @ToolParam(description = "Internal category ID resolved from get_all_primary_categories. Never ask the user for this.") String categoryId,
            @ToolParam(description = "The new budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart,
            ToolContext toolContext) {
        String authorization = authorization(toolContext);
        if (authorization == null) {
            return ToolResponse.failure("Cannot update the budget because the caller authorization is unavailable.");
        }
        UUID resolvedBudgetId = parseUuid(budgetId);
        UUID resolvedCategoryId = parseUuid(categoryId);
        if (resolvedBudgetId == null) {
            return ToolResponse.failure("The budgetId is invalid. Call get_budgets and use an exact returned budget ID.");
        }
        if (resolvedCategoryId == null) {
            return ToolResponse.failure("The categoryId is invalid. Call get_all_primary_categories and use an exact returned category ID.");
        }
        LocalDate resolvedPeriodStart = parsePeriodStart(periodStart);
        if (resolvedPeriodStart == null) {
            return ToolResponse.failure("The periodStart must use yyyy-MM-dd format, or be omitted for the current month.");
        }
        BudgetResponseDto budget = budgetingClient.updateBudget(
                authorization,
                resolvedBudgetId,
                new CreateBudgetRequestDto(
                        resolvedCategoryId,
                        limitAmount,
                        isoCurrencyCode,
                        normalizePeriod(period),
                        resolvedPeriodStart))
                .getBody();
        return ToolResponse.success("Budget updated.", budget);
    }

    @Tool(
        name = "deactivate_budget",
        description = """
            Deactivates an existing budget by its ID. \
            Only call this after the user has explicitly confirmed they want the budget removed."""
    )
    public ToolResponse<Void> deactivateBudget(
            @ToolParam(description = "The ID of the budget to deactivate.") String budgetId,
            ToolContext toolContext) {
        String authorization = authorization(toolContext);
        if (authorization == null) {
            return ToolResponse.failure("Cannot deactivate the budget because the caller authorization is unavailable.");
        }
        UUID resolvedBudgetId = parseUuid(budgetId);
        if (resolvedBudgetId == null) {
            return ToolResponse.failure("The budgetId is invalid. Call get_budgets and use an exact returned budget ID.");
        }
        budgetingClient.deactivateBudget(authorization, resolvedBudgetId);
        return ToolResponse.success("Budget deactivated.", null);
    }

    private String authorization(ToolContext toolContext) {
        Object value = toolContext.getContext().get(ToolContextKeys.AUTHORIZATION);
        return value instanceof String authorization && !authorization.isBlank() ? authorization : null;
    }

    private String normalizePeriod(String period) {
        return period == null || period.isBlank() ? "MONTHLY" : period;
    }

    private LocalDate parsePeriodStart(String periodStart) {
        if (periodStart == null || periodStart.isBlank()) {
            return LocalDate.now().withDayOfMonth(1);
        }
        try {
            return LocalDate.parse(periodStart);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
