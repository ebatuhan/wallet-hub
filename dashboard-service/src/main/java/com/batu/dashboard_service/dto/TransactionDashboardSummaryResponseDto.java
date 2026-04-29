package com.batu.dashboard_service.dto;

import java.util.UUID;

import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.TransactionDto;

public record TransactionDashboardSummaryResponseDto(
        UUID userId,
        TransactionDto transaction,
        AccountResponseDto account
) {}
