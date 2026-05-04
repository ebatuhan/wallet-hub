package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryDto(
        UUID primaryCategoryId,
        BigDecimal percentageOfCurrencySpending,
        BigDecimal amountSpent) {
}
