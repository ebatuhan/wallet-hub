package com.batu.dashboard_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record AccountDashboardSummaryResponseDto(
        UUID userId,
        UserDashboardSummaryResponseDto.PeriodDto period,
        AccountResponseDto account,
        List<AccountBalanceDataPointDto> balanceHistory,
        SpendingSectionDto spending,
        UserDashboardSummaryResponseDto.RecentTransactionsDto transactions
) {
    public record SpendingSectionDto(BigDecimal totalSpent, List<SpendingCategoryItemDto> categories,
            SpendingGraphResponseDto graph) {}
}
