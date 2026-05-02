package com.batu.budgeting.service.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ApplyTransactionInput(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        String isoCurrencyCode,
        LocalDate date,
        Boolean pending,
        UUID primaryCategoryId,
        boolean active) {
}
