package com.batu.insights_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryResponseDTO(
        UUID userId,
        BigDecimal totalSpent,
        List<SpendingPerCategoryDTO> categories
) {}
