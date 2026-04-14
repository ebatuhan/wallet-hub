package com.batu.ai_assistant.dto.client;

import java.util.UUID;

public record TransactionPrimaryCategoryDto(
        UUID transactionPrimaryCategoryId,
        String categoryCode,
        String displayName,
        String iconUrl
) {}
