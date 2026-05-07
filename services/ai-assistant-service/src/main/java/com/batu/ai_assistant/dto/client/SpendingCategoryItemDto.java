package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingCategoryItemDto(
        UUID primaryCategoryId,
        String primaryCategoryCode,
        String primaryCategoryDisplayName,
        String primaryCategoryIconUrl,
        BigDecimal percentageOfCurrencySpending,
        BigDecimal amountSpent
) {}
