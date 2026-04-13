package com.batu.dashboard_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingCategoryItemDto(
        UUID primaryCategoryId,
        String primaryCategoryCode,
        String primaryCategoryDisplayName,
        String primaryCategoryIconUrl,
        BigDecimal percentage,
        BigDecimal totalAmount
) {}
