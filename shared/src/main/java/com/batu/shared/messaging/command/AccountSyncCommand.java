package com.batu.shared.messaging.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountSyncCommand {

    private UUID accountId;
    private UUID userId;
    private String institutionName;
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
