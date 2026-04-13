package com.batu.dashboard_service.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.dashboard_service.dto.client.BudgetResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record UserDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountSummaryResponseDto accounts,
        RecentTransactionsDto recentTransactions,
        SpendingSectionDto spending,
        BudgetHighlightsDto budgets
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasNext, String nextCursor) {}

    public record SpendingSectionDto(List<SpendingCategoryItemDto> categories) {}

    public record BudgetHighlightsDto(long activeBudgetCount, long overBudgetCount, List<BudgetResponseDto> items) {}
}
