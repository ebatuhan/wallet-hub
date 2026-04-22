package com.batu.shared.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountViewDto {

    private UUID accountId;
    private String institutionName;
    private String accountName;
    private BigDecimal currentBalance;
    private String accountType;
    private String accountMask;
    private Instant createdAt;
}
