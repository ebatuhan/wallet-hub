package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountBalanceDataPointDto(
        UUID accountId,
        UUID userId,
        BigDecimal balance,
        String isoCurrencyCode,
        LocalDate date
) {}
