package com.batu.insights_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountBalanceDataPointRow(
    UUID accountId,
    UUID userId,
    BigDecimal balance,
    String isoCurrencyCode,
    LocalDate date
) {

}
