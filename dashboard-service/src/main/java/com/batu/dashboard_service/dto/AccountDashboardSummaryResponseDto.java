package com.batu.dashboard_service.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.batu.dashboard_service.dto.client.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public record AccountDashboardSummaryResponseDto(
        UUID userId,
        UserDashboardSummaryResponseDto.PeriodDto period,
        AccountResponseDto account,
        List<AccountBalanceDataPointDto> balanceHistory,
        UserDashboardSummaryResponseDto.SpendingSectionDto spending,
        UserDashboardSummaryResponseDto.RecentTransactionsDto recentTransactions
) {}
