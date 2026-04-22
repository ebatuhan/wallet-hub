package com.batu.budgeting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.batu.budgeting.entity.BudgetPeriod;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String categoryDisplayName,
        String categoryIconUrl,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        String isoCurrencyCode,
        BudgetPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean active
) {}
