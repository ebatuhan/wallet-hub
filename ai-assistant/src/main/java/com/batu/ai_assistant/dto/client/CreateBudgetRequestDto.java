package com.batu.ai_assistant.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBudgetRequestDto(
        UUID categoryId,
        BigDecimal limitAmount,
        String isoCurrencyCode,
        String period,
        LocalDate periodStart
) {}
