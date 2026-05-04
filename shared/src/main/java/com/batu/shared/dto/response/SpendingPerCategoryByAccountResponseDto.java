package com.batu.shared.dto.response;

import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryByAccountResponseDto(
        UUID userId,
        UUID accountId,
        List<SpendingCurrencyGroupDto> spendingByCurrency) {
}
