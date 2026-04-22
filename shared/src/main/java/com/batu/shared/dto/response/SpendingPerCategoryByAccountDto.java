package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingPerCategoryByAccountDto(
        UUID primaryCategoryId,
        BigDecimal percentage,
        BigDecimal totalAmount) {
}
