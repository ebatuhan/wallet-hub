package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record AccountDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountDto account,
        List<AccountBalanceDataPointDto> balanceHistory,
        SpendingSectionDto spending,
        RecentTransactionsDto transactions
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record AccountDto(UUID accountId, String institutionName, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode) {}

    public record SpendingSectionDto(List<UserDashboardSummaryResponseDto.SpendingCurrencyGroupDto> categoryBreakdownByCurrency,
            SpendingGraphResponseDto trendByCurrency) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasMore, String nextCursor) {}
}
