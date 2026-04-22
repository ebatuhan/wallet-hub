package com.batu.dashboard_service.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;

public interface DashboardService {
    UserDashboardSummaryResponseDto getUserSummary(LocalDate from, LocalDate to, Integer recentLimit, Jwt principal);

    AccountDashboardSummaryResponseDto getAccountSummary(UUID accountId, LocalDate from, LocalDate to, Integer recentLimit,
            Jwt principal);
}
