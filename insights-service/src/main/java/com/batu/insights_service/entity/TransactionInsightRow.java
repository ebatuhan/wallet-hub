package com.batu.insights_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionInsightRow(
    LocalDate date,
    String primaryCategoryCode,
    String paymentChannel,
    BigDecimal amount,
    boolean isOutflow,
    boolean isActive,
    String isoCurrencyCode,
    UUID userId,
    UUID accountId,
    UUID transactionId,
    Instant updatedAt
) {}
