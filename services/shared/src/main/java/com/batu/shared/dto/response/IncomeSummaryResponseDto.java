package com.batu.shared.dto.response;

import java.util.List;
import java.util.UUID;

public record IncomeSummaryResponseDto(
        UUID userId,
        List<IncomeTotalByCurrencyDto> totalsByCurrency) {
}
