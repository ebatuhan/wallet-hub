package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SpendingCurrencyGroupDto(
        String isoCurrencyCode,
        BigDecimal totalSpent,
        List<SpendingPerCategoryDto> categories) {
}
