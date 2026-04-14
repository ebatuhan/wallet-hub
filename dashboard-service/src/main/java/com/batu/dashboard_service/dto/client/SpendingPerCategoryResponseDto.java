package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryResponseDto(
        UUID userId,
        BigDecimal totalSpent,
        List<SpendingPerCategoryDto> categories
) {}
