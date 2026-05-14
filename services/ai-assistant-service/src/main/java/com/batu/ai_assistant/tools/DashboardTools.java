package com.batu.ai_assistant.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.dto.client.AccountDashboardSummaryResponseDto;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;

@Component
public class DashboardTools {

    private final DashboardClient dashboardClient;

    public DashboardTools(DashboardClient dashboardClient) {
        this.dashboardClient = dashboardClient;
    }

    @Tool(
        name = "get_dashboard_summary",
        description = """
            Returns the user's overall financial summary including income, expenses, and category breakdowns for a year or month. Use it to analyse users finance information. \
            Spending amounts are grouped by isoCurrencyCode; never sum totals or category amounts across currency groups. \
            The from parameter accepts YYYY for a yearly summary or YYYY-MM for a monthly summary. When omitted the current month is used automatically — do not mention this to the user. \
            Call this proactively and silently whenever the user asks about their spending, finances, or before suggesting budgets. \
            Analyse the response of this carefully as if you are a professional personal finance manager, try to make suggestions from results of this tool if user asks.  \
            Never ask the user for a date range before calling this — choose YYYY, YYYY-MM, or use the default."""
    )
    public ToolResponse<UserDashboardSummaryResponseDto> getDashboardSummary(
            @ToolParam(required = false, description = "Period in YYYY or YYYY-MM format. Omit to use the current month.") String from) {
        UserDashboardSummaryResponseDto summary = dashboardClient.getSummary(from).getBody();
        if (summary == null) {
            return ToolResponse.failure("Dashboard summary unavailable.");
        }
        return ToolResponse.success("Dashboard summary loaded.", summary);
    }

    @Tool(
        name = "get_account_dashboard_summary",
        description = """
            Returns the financial summary for a single specific account for a year or month. \
            Spending amounts are grouped by isoCurrencyCode; never sum totals or category amounts across currency groups. \
            Use this only when the user is asking about one particular account rather than their overall finances. \
            The from parameter accepts YYYY for a yearly summary or YYYY-MM for a monthly summary. When omitted the current month is used automatically — do not mention this to the user."""
    )
    public ToolResponse<AccountDashboardSummaryResponseDto> getAccountDashboardSummary(
            @ToolParam(description = "The ID of the account to summarize.") java.util.UUID accountId,
            @ToolParam(required = false, description = "Period in YYYY or YYYY-MM format. Omit to use the current month.") String from,
            @ToolParam(required = false, description = "Maximum number of transactions to return. Omit to use the dashboard default.") Integer limit,
            @ToolParam(required = false, description = "Pagination cursor for transactions. Omit for the first page.") String cursor) {
        AccountDashboardSummaryResponseDto summary = dashboardClient.getAccountSummary(
                accountId,
                from,
                limit,
                cursor)
                .getBody();
        if (summary == null) {
            return ToolResponse.failure("Account dashboard summary unavailable.");
        }
        return ToolResponse.success("Account dashboard summary loaded.", summary);
    }
}
