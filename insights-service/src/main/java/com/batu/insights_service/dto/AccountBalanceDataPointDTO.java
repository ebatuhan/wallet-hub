package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountBalanceDataPointDTO(
        UUID accountId,
        UUID userId,
        BigDecimal balance,
        String isoCurrencyCode,
        LocalDate date
) {
}