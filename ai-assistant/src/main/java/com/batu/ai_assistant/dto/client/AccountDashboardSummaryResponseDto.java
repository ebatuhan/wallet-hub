package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountDashboardSummaryResponseDto(
        UUID userId,
        PeriodDto period,
        AccountDto account,
        List<AccountBalanceDataPointDto> balanceHistory,
        SpendingSectionDto spending,
        RecentTransactionsDto recentTransactions
) {
    public record PeriodDto(LocalDate from, LocalDate to) {}

    public record AccountDto(UUID accountId, String institutionName, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode) {}

    public record BalancePointDto(LocalDate date, BigDecimal balance) {}

    public record SpendingSectionDto(List<SpendingCategoryItemDto> categories) {}

    public record RecentTransactionsDto(List<TransactionViewResponseDto> items, boolean hasNext, String nextCursor) {}
}
