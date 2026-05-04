package com.batu.shared.dto.response;

import java.util.List;

public record SpendingGraphSeriesDto(
        String isoCurrencyCode,
        List<SpendingGraphPointDto> spendingPoints) {
}
