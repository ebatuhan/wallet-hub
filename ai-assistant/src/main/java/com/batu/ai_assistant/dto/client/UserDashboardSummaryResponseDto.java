package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record UserDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountSummaryDto accounts,
        IncomeSectionDto income,
        RecentTransactionsDto recentTransactions,
        SpendingSectionDto spending,
        BudgetHighlightsDto budgets
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record AccountSummaryDto(long activeAccountCount, List<AccountCurrencyTotalDto> totalsByCurrency) {}

    public record AccountCurrencyTotalDto(String isoCurrencyCode, BigDecimal currentBalanceTotal,
            BigDecimal availableBalanceTotal) {}

    public record IncomeSectionDto(List<IncomeTotalByCurrencyDto> totalsByCurrency) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasMore, String nextCursor) {}

    public record SpendingSectionDto(List<SpendingCurrencyGroupDto> currencies,
            SpendingGraphResponseDto yearlySpendings) {}

    public record SpendingCurrencyGroupDto(String isoCurrencyCode, BigDecimal totalSpent,
            List<SpendingCategoryItemDto> categories) {}

    public record BudgetHighlightsDto(long activeBudgetCount, long overBudgetCount, List<BudgetResponseDto> items) {}
}
