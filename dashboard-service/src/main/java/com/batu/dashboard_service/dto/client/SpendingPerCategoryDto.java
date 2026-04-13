package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryDto(
        UUID primaryCategoryId,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}
