package com.batu.budgeting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.batu.budgeting.entity.BudgetPeriod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBudgetRequest(
        @NotNull UUID categoryId,
        @NotNull @Positive BigDecimal limitAmount,
        @NotBlank @Size(min = 3, max = 3) String isoCurrencyCode,
        @NotNull BudgetPeriod period,
        @NotNull LocalDate periodStart
) {}
