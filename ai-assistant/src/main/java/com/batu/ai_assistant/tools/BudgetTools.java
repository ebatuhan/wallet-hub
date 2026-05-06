package com.batu.ai_assistant.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.client.BudgetingClient;
import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.client.TransactionCategoryClient;
import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BudgetTools {

    private final BudgetingClient budgetingClient;
    private final DashboardClient dashboardClient;
    private final TransactionCategoryClient transactionCategoryClient;

    @Tool(
        name = "get_budgets",
        description = """
            Returns active budgets for the user with cursor pagination. \
            Call this proactively whenever the user asks about budgets or before suggesting new ones \
            to avoid duplicating an already existing budget."""
    )
    public ToolResponse<CursorResponse<BudgetResponseDto>> getBudgets(
            @ToolParam(required = false, description = "Maximum number of budgets to return. Default: 10.") Integer limit,
            @ToolParam(required = false, description = "Cursor from a previous budget page.") String cursor) {
        return ToolResponse.success(
                "Active enriched budgets loaded.",
                dashboardClient.getBudgets(limit, cursor, null, null).getBody());
    }

    @Tool(
        name = "create_budget",
        description = """
            Creates a new budget for a category. \
            Always resolve the categoryCode silently using get_all_primary_categories before calling this — never expose this step to the user. \
            If period is omitted, default to MONTHLY. \
            If periodStart is omitted, default to the first day of the current month. \
            Only call this after the user has explicitly confirmed they want the budget created."""
    )
    public ToolResponse<BudgetResponseDto> createBudget(
            @ToolParam(description = "Primary category code resolved from get_all_primary_categories, e.g. FOOD_AND_DRINK. Never ask the user for this.") String categoryCode,
            @ToolParam(description = "The budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart) {
        UUID categoryId = resolveCategoryId(categoryCode);
        if (categoryId == null) {
            return ToolResponse.failure("Unknown budget category code. Load primary categories and use one of their categoryCode values.");
        }
        LocalDate resolvedPeriodStart = parsePeriodStart(periodStart);
        if (resolvedPeriodStart == null) {
            return ToolResponse.failure("The periodStart must use yyyy-MM-dd format, or be omitted for the current month.");
        }
        BudgetResponseDto budget = budgetingClient.createBudget(
                new CreateBudgetRequestDto(
                        categoryId,
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
            Always resolve the categoryCode silently using get_all_primary_categories and call get_budgets before calling this to see the user's current budgets. Never expose this step to the user. \
            If period is omitted, default to MONTHLY. \
            If periodStart is omitted, default to the first day of the current month. \
            Only call this after the user has explicitly confirmed they want the change applied."""
    )
    public ToolResponse<BudgetResponseDto> updateBudget(
            @ToolParam(description = "The ID of the budget to update, copied from get_budgets.") String budgetId,
            @ToolParam(description = "Primary category code resolved from get_all_primary_categories, e.g. FOOD_AND_DRINK. Never ask the user for this.") String categoryCode,
            @ToolParam(description = "The new budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart) {
        UUID resolvedBudgetId = parseUuid(budgetId);
        if (resolvedBudgetId == null) {
            return ToolResponse.failure("Invalid budget ID. Load budgets and use an ID from get_budgets.");
        }
        UUID categoryId = resolveCategoryId(categoryCode);
        if (categoryId == null) {
            return ToolResponse.failure("Unknown budget category code. Load primary categories and use one of their categoryCode values.");
        }
        LocalDate resolvedPeriodStart = parsePeriodStart(periodStart);
        if (resolvedPeriodStart == null) {
            return ToolResponse.failure("The periodStart must use yyyy-MM-dd format, or be omitted for the current month.");
        }
        BudgetResponseDto budget = budgetingClient.updateBudget(
                resolvedBudgetId,
                new CreateBudgetRequestDto(
                        categoryId,
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
            @ToolParam(description = "The ID of the budget to deactivate, copied from get_budgets.") String budgetId) {
        UUID resolvedBudgetId = parseUuid(budgetId);
        if (resolvedBudgetId == null) {
            return ToolResponse.failure("Invalid budget ID. Load budgets and use an ID from get_budgets.");
        }
        budgetingClient.deactivateBudget(resolvedBudgetId);
        return ToolResponse.success("Budget deactivated.", null);
    }

    private UUID resolveCategoryId(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        String normalizedCategoryCode = categoryCode.trim();
        return transactionCategoryClient.getAllPrimaryCategories().getBody().stream()
                .filter(category -> normalizedCategoryCode.equalsIgnoreCase(category.getCategoryCode()))
                .map(TransactionPrimaryCategoryDto::getTransactionPrimaryCategoryId)
                .findFirst()
                .orElse(null);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

}
