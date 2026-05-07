package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpendingGraphAggregate(
        String isoCurrencyCode,
        LocalDate bucket,
        BigDecimal totalAmount) {
}
