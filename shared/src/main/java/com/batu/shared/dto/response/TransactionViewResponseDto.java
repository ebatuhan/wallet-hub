package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionViewResponseDto {

    private UUID transactionId;
    private BigDecimal amount;
    private String transactionName;
    private String transactionType;
    private LocalDate date;
    private Boolean pending;
    private String paymentChannel;
    private String isoCurrencyCode;
    private UUID primaryCategoryId;
    private String primaryCategoryCode;
    private String categoryDisplayName;
    private String primaryCategoryIconUrl;
    private UUID detailedCategoryId;
    private String detailedCategoryCode;
    private String detailedCategoryName;
    private UUID accountId;
}
