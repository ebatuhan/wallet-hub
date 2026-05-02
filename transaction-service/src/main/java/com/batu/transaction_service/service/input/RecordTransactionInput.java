package com.batu.transaction_service.service.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordTransactionInput(
        UUID transactionId,
        UUID userId,
        UUID accountId,
        BigDecimal amount,
        String isoCurrencyCode,
        String transactionName,
        String transactionType,
        LocalDate date,
        Boolean pending,
        String paymentChannel,
        String detailedCategoryCode,
        boolean active,
        long version) {
}
