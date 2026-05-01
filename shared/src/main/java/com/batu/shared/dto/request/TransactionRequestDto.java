package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {

    private UUID transactionId;
    private UUID userId;
    private UUID accountId;
    private BigDecimal amount;
    private String isoCurrencyCode;
    private String transactionName;
    private String transactionType;
    private LocalDate date;
    private Boolean pending = Boolean.FALSE;
    private String paymentChannel;
    private String detailedCategoryCode;
    @JsonProperty("isActive")
    private boolean isActive = true;
    private long syncVersion;

    public TransactionRequestDto(UUID transactionId, UUID userId, UUID accountId, BigDecimal amount, String isoCurrencyCode,
            String transactionName, String transactionType, LocalDate date, Boolean pending, String paymentChannel,
            String detailedCategoryCode, boolean isActive) {
        this(transactionId, userId, accountId, amount, isoCurrencyCode, transactionName, transactionType, date, pending,
                paymentChannel, detailedCategoryCode, isActive, 0L);
    }
}
