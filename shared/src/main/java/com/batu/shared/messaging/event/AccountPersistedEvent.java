package com.batu.shared.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountPersistedEvent {

    private UUID eventId;
    private Instant occurredAt;
    private String sourceService;
    private UUID accountId;
    private UUID connectionId;
    private UUID userId;
    private String externalId;
    private String accountName;
    private String accountType;
    private String accountSubtype;
    private String accountMask;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private String isoCurrencyCode;
    @JsonProperty("isActive")
    private boolean isActive;
}
