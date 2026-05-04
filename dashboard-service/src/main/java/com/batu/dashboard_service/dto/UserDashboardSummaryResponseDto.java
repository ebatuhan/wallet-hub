package com.batu.dashboard_service.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.IncomeTotalByCurrencyDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record UserDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountSummaryResponseDto accounts,
        IncomeSectionDto income,
        RecentTransactionsDto recentTransactions,
        SpendingSectionDto spending
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasMore, String nextCursor) {}

    public record IncomeSectionDto(List<IncomeTotalByCurrencyDto> totalsByCurrency) {}

    public record SpendingSectionDto(List<SpendingCurrencyGroupDto> categoryBreakdownByCurrency,
            SpendingGraphResponseDto yearlyTrendByCurrency) {}

    public record SpendingCurrencyGroupDto(String isoCurrencyCode, java.math.BigDecimal totalSpent,
            List<SpendingCategoryItemDto> categoryBreakdown) {}
}
