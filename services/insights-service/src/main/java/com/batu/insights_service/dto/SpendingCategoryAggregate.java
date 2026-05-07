package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingCategoryAggregate(
        String isoCurrencyCode,
        UUID primaryCategoryId,
        BigDecimal percentage,
        BigDecimal totalAmount) {
}
