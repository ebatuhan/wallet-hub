package com.batu.insights_service.dto;

import java.util.List;
import java.util.UUID;

public record SpendingPerCategoryByAccountResponseDTO(
        UUID userId,
        UUID accountId,
        List<SpendingPerCategoryByAccountDTO> categories
) {}
