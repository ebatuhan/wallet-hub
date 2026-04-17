package com.batu.shared.dto.request;

import java.math.BigDecimal;
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
public class AccountRequestDto {

    private UUID accountId;
    private UUID connectionId;
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
    private boolean isActive = true;
}
