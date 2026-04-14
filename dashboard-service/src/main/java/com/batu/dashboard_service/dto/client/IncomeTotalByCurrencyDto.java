package com.batu.dashboard_service.dto.client;

import java.math.BigDecimal;

public record IncomeTotalByCurrencyDto(
        String isoCurrencyCode,
        BigDecimal totalIncome
) {}
