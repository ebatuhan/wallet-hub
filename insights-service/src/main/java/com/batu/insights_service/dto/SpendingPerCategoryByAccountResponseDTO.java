package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryByAccountResponseDTO(
        UUID userId,
        UUID accountId,
        BigDecimal totalSpent,
        List<SpendingPerCategoryByAccountDTO> categories
) {}
