package com.batu.dashboard_service.dto.client;

import java.util.List;
import java.util.UUID;

public record IncomeSummaryResponseDto(
        UUID userId,
        List<IncomeTotalByCurrencyDto> totalsByCurrency
) {}
