package com.batu.shared.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPersistedEvent {

    private UUID eventId;
    private Instant occurredAt;
    private String sourceService;
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
    private UUID primaryCategoryId;
    private String primaryCategoryCode;
    @JsonProperty("isActive")
    private boolean isActive;
    private long syncVersion;

    public TransactionPersistedEvent(UUID eventId, Instant occurredAt, String sourceService, UUID transactionId,
            UUID userId, UUID accountId, BigDecimal amount, String isoCurrencyCode, String transactionName,
            String transactionType, LocalDate date, Boolean pending, String paymentChannel, UUID primaryCategoryId,
            String primaryCategoryCode, boolean isActive) {
        this(eventId, occurredAt, sourceService, transactionId, userId, accountId, amount, isoCurrencyCode,
                transactionName, transactionType, date, pending, paymentChannel, primaryCategoryId, primaryCategoryCode,
                isActive, 0L);
    }
}
