package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponseDto(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String categoryDisplayName,
        String categoryIconUrl,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        String isoCurrencyCode,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean active
) {}
