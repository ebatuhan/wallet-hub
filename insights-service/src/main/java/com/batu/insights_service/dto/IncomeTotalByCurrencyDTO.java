package com.batu.insights_service.dto;

import java.math.BigDecimal;

public record IncomeTotalByCurrencyDTO(
        String isoCurrencyCode,
        BigDecimal totalIncome
) {}
