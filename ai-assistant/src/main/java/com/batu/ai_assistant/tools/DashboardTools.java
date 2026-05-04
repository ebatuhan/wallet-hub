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
            Returns the user's overall financial summary including income, expenses, and category breakdowns for a date range. Use it to analyse users finance information. \
            When dates are omitted the current month is used automatically — do not mention this to the user. \
            Call this proactively and silently whenever the user asks about their spending, finances, or before suggesting budgets. \
            Analyse the response of this carefully as if you are a professional personal finance manager, try to make suggestions from results of this tool if user asks.  \
            Never ask the user for a date range before calling this — just use the defaults."""
    )
    public ToolResponse<UserDashboardSummaryResponseDto> getDashboardSummary(
            @ToolParam(required = false, description = "Start date in yyyy-MM-dd format. Omit to use the first day of the current month.") String from,
            @ToolParam(required = false, description = "End date in yyyy-MM-dd format. Omit to use today's date.") String to) {
        return ToolResponse.success("Dashboard summary loaded.", dashboardClient.getSummary(from, to).getBody());
    }

    @Tool(
        name = "get_account_dashboard_summary",
        description = """
            Returns the financial summary for a single specific account in a date range. \
            Use this only when the user is asking about one particular account rather than their overall finances. \
            When dates are omitted the current month is used automatically — do not mention this to the user."""
    )
    public ToolResponse<AccountDashboardSummaryResponseDto> getAccountDashboardSummary(
            @ToolParam(description = "The ID of the account to summarize.") java.util.UUID accountId,
            @ToolParam(required = false, description = "Start date in yyyy-MM-dd format. Omit to use the first day of the current month.") String from,
            @ToolParam(required = false, description = "End date in yyyy-MM-dd format. Omit to use today's date.") String to,
            @ToolParam(required = false, description = "Maximum number of transactions to return. Omit to use the dashboard default.") Integer limit,
            @ToolParam(required = false, description = "Pagination cursor for transactions. Omit for the first page.") String cursor) {
        return ToolResponse.success("Account dashboard summary loaded.", dashboardClient.getAccountSummary(
                accountId,
                from,
                to,
                limit,
                cursor)
                .getBody());
    }
}
