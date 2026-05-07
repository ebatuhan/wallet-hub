package com.batu.shared.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SpendingGraphResponseDto(
        UUID userId,
        UUID accountId,
        String groupBy,
        LocalDate from,
        LocalDate to,
        List<SpendingGraphSeriesDto> seriesByCurrency) {
}
