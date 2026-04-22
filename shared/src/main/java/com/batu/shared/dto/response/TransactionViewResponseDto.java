package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionViewResponseDto {

    private UUID transactionId;
    private BigDecimal amount;
    private String transactionName;
    private String isoCurrencyCode;
    private String categoryDisplayName;
    private String detailedCategoryName;
    private UUID accountId;
    private String accountName;
}
