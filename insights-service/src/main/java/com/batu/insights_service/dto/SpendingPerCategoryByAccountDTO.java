package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryByAccountDTO(
        UUID primaryCategoryId,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}
