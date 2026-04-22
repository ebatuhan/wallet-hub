package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpendingGraphPointDto(
        LocalDate bucket,
        BigDecimal totalAmount) {
}
