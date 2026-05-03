package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpsertRequestDto {
    private UUID transactionId;
    private UUID userId;
    private UUID accountId;
    private BigDecimal amount;
    private String isoCurrencyCode;
    private String transactionName;
    private String transactionType;
    private LocalDate date;
    private Boolean pending;
    private String paymentChannel;
    private String detailedCategoryCode;
    private boolean active;
}
