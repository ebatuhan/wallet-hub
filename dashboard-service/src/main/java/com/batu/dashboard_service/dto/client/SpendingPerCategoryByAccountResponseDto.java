package com.batu.dashboard_service.dto.client;

import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryByAccountResponseDto(
        UUID userId,
        UUID accountId,
        List<SpendingPerCategoryByAccountDto> categories
) {}
