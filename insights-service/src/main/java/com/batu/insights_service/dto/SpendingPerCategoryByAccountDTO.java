package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryByAccountDTO(
        UUID accountId,
        UUID userId,
        String primaryCategoryCode,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}