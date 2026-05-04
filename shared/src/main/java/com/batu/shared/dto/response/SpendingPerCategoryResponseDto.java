package com.batu.shared.dto.response;

import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryResponseDto(
        UUID userId,
        List<SpendingCurrencyGroupDto> currencies) {
}
