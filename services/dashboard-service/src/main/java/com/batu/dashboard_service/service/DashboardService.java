package com.batu.dashboard_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.TransactionDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;

public interface DashboardService {
    UserDashboardSummaryResponseDto getUserSummary(LocalDate from, LocalDate to, Integer recentLimit, Jwt principal);

    CursorResponse<BudgetResponseDto> getBudgets(Integer limit, String cursor, String sortBy, String direction,
            Jwt principal);

    AccountDashboardSummaryResponseDto getAccountSummary(UUID accountId, LocalDate from, LocalDate to, Integer limit,
            String cursor, Jwt principal);

    TransactionDashboardSummaryResponseDto getTransactionSummary(UUID transactionId, Jwt principal);
}
