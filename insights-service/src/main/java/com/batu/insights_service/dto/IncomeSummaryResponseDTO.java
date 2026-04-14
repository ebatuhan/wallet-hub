package com.batu.insights_service.dto;

import java.util.List;
import java.util.UUID;

public record IncomeSummaryResponseDTO(
        UUID userId,
        List<IncomeTotalByCurrencyDTO> totalsByCurrency
) {}
