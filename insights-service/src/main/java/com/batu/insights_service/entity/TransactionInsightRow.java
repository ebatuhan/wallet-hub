package com.batu.insights_service.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionInsightRow(
    LocalDate date,
    String primaryCategoryCode,
    String paymentChannel,
    BigDecimal amount,
    String isoCurrencyCode,
    UUID userId,
    UUID accountId,
    UUID transactionId
) {}
