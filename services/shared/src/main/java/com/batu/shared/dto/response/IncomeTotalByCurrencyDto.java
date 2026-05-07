package com.batu.shared.dto.response;

import java.math.BigDecimal;

public record IncomeTotalByCurrencyDto(
        String isoCurrencyCode,
        BigDecimal totalIncome) {
}
