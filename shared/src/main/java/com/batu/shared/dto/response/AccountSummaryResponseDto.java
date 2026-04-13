package com.batu.shared.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountSummaryResponseDto {
    private UUID userId;
    private long activeAccountCount;
    private List<AccountCurrencyTotalDto> totalsByCurrency;
}
