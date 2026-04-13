package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryByAccountDto(
        UUID primaryCategoryId,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}
