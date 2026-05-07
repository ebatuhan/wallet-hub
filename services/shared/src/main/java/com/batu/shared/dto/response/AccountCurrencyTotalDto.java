package com.batu.shared.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountCurrencyTotalDto {
    private String isoCurrencyCode;
    private BigDecimal currentBalanceTotal;
    private BigDecimal availableBalanceTotal;
}
