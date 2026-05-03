package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpsertRequestDto {
    private UUID accountId;
    private UUID userId;
    private UUID connectionId;
    private String institutionName;
    private String accountName;
    private String accountType;
    private String accountSubtype;
    private String accountMask;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private String isoCurrencyCode;
}
