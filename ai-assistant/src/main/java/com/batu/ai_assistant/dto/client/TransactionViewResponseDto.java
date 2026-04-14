package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionViewResponseDto(
        UUID transactionId,
        BigDecimal amount,
        String transactionName,
        String isoCurrencyCode,
        String categoryDisplayName,
        String detailedCategoryName,
        UUID accountId,
        String accountName
) {}
