package com.batu.insights_service.dto;

import java.math.BigDecimal;

public record SpendingPerCategoryByAccountDTO(
        String primaryCategoryCode,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}
