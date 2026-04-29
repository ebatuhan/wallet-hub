package com.batu.dashboard_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record UserDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountSummaryResponseDto accounts,
        IncomeSectionDto income,
        RecentTransactionsDto recentTransactions,
        SpendingSectionDto spending,
        BudgetHighlightsDto budgets
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasMore, String nextCursor) {}

    public record IncomeSectionDto(List<IncomeTotalByCurrencyDto> totalsByCurrency) {}

    public record SpendingSectionDto(BigDecimal totalSpent, List<SpendingCategoryItemDto> categories,
            SpendingGraphResponseDto yearlySpendings) {}

    public record BudgetHighlightsDto(long activeBudgetCount, long overBudgetCount, List<BudgetResponseDto> items) {}
}
