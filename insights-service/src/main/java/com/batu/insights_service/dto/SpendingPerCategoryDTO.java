package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.UUID;


public record SpendingPerCategoryDTO(
        UUID userId,
        String primaryCategoryCode,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}

