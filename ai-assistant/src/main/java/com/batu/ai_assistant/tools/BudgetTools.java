package com.batu.ai_assistant.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.client.BudgetingClient;
import com.batu.ai_assistant.dto.client.BudgetResponseDto;
import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;

@Component
public class BudgetTools {

    private final BudgetingClient budgetingClient;

    public BudgetTools(BudgetingClient budgetingClient) {
        this.budgetingClient = budgetingClient;
    }

    @Tool(
        name = "get_budgets",
        description = """
            Returns all active budgets for the user. \
            Call this proactively whenever the user asks about budgets or before suggesting new ones \
            to avoid duplicating an already existing budget."""
    )
    public java.util.List<BudgetResponseDto> getBudgets(ToolContext toolContext) {
        return budgetingClient.getBudgets(authorization(toolContext)).getBody();
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
    public BudgetResponseDto createBudget(
            @ToolParam(description = "Internal category ID resolved from get_all_primary_categories. Never ask the user for this.") String categoryId,
            @ToolParam(description = "The budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart,
            ToolContext toolContext) {
        return budgetingClient.createBudget(
                authorization(toolContext),
                new CreateBudgetRequestDto(
                        UUID.fromString(categoryId),
                        limitAmount,
                        isoCurrencyCode,
                        normalizePeriod(period),
                        resolvePeriodStart(periodStart)))
                .getBody();
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
    public BudgetResponseDto updateBudget(
            @ToolParam(description = "The ID of the budget to update.") String budgetId,
            @ToolParam(description = "Internal category ID resolved from get_all_primary_categories. Never ask the user for this.") String categoryId,
            @ToolParam(description = "The new budget spending limit as a number.") BigDecimal limitAmount,
            @ToolParam(description = "Three-letter ISO currency code, e.g. USD or EUR.") String isoCurrencyCode,
            @ToolParam(required = false, description = "Budget period. Default: MONTHLY.") String period,
            @ToolParam(required = false, description = "Budget start date in yyyy-MM-dd format. Default: first day of the current month.") String periodStart,
            ToolContext toolContext) {
        return budgetingClient.updateBudget(
                authorization(toolContext),
                UUID.fromString(budgetId),
                new CreateBudgetRequestDto(
                        UUID.fromString(categoryId),
                        limitAmount,
                        isoCurrencyCode,
                        normalizePeriod(period),
                        resolvePeriodStart(periodStart)))
                .getBody();
    }

    @Tool(
        name = "deactivate_budget",
        description = """
            Deactivates an existing budget by its ID. \
            Only call this after the user has explicitly confirmed they want the budget removed."""
    )
    public String deactivateBudget(
            @ToolParam(description = "The ID of the budget to deactivate.") String budgetId,
            ToolContext toolContext) {
        budgetingClient.deactivateBudget(authorization(toolContext), UUID.fromString(budgetId));
        return "Budget deactivated.";
    }

    private String authorization(ToolContext toolContext) {
        return toolContext.getContext().get("authorization").toString();
    }

    private String normalizePeriod(String period) {
        return period == null || period.isBlank() ? "MONTHLY" : period;
    }

    private LocalDate resolvePeriodStart(String periodStart) {
        return periodStart == null || periodStart.isBlank()
                ? LocalDate.now().withDayOfMonth(1)
                : LocalDate.parse(periodStart);
    }
}