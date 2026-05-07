package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountResponseDto {

    private UUID accountId;
    private String institutionName;
    private String accountName;
    private String accountType;
    private String accountSubtype;
    private String accountMask;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private String isoCurrencyCode;
    private Instant createdAt;
    private Instant updatedAt;

    @java.beans.ConstructorProperties({ "accountId", "institutionName", "accountName", "accountType",
            "accountSubtype", "accountMask", "currentBalance", "availableBalance", "isoCurrencyCode", "createdAt",
            "updatedAt" })
    public AccountResponseDto(UUID accountId, String institutionName, String accountName, String accountType,
            String accountSubtype, String accountMask, BigDecimal currentBalance, BigDecimal availableBalance,
            String isoCurrencyCode, Instant createdAt, Instant updatedAt) {
        this.accountId = accountId;
        this.institutionName = institutionName;
        this.accountName = accountName;
        this.accountType = accountType;
        this.accountSubtype = accountSubtype;
        this.accountMask = accountMask;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.isoCurrencyCode = isoCurrencyCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
