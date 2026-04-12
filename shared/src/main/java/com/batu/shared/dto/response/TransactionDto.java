package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionDto {

    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String isoCurrencyCode;
    private String transactionName;
    private String transactionType;
    private LocalDate date;
    private Boolean pending;
    private String paymentChannel;
    private TransactionDetailedCategoryDto detailedCategory;
    private Instant createdAt;
    private Instant updatedAt;
}
