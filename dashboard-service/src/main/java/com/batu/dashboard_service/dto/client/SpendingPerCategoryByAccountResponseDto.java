package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryByAccountResponseDto(
        UUID userId,
        UUID accountId,
        BigDecimal totalSpent,
        List<SpendingPerCategoryByAccountDto> categories
) {}
