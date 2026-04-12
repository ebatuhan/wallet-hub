package com.batu.insights_service.dto;

import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryResponseDTO(
        UUID userId,
        List<SpendingPerCategoryDTO> categories
) {}
