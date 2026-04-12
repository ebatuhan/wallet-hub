package com.batu.shared.messaging.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSyncCommand {

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
    @JsonProperty("isActive")
    private boolean isActive;
}
