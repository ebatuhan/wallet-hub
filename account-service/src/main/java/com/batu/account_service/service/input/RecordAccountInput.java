package com.batu.account_service.service.input;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordAccountInput(
        UUID accountId,
        UUID userId,
        UUID connectionId,
        String institutionName,
        String accountName,
        String accountType,
        String accountSubtype,
        String accountMask,
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        String isoCurrencyCode,
        long version) {
}
