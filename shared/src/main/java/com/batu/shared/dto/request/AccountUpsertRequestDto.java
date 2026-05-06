package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpsertRequestDto {
    @NotNull(message = "Account id is required")
    private UUID accountId;

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Connection id is required")
    private UUID connectionId;

    @NotBlank(message = "Institution name is required")
    private String institutionName;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Account type is required")
    private String accountType;

    private String accountSubtype;

    @NotBlank(message = "Account mask is required")
    private String accountMask;

    @NotNull(message = "Current balance is required")
    private BigDecimal currentBalance;

    @NotNull(message = "Available balance is required")
    private BigDecimal availableBalance;

    @NotBlank(message = "ISO currency code is required")
    @Size(min = 3, max = 3, message = "ISO currency code must be 3 characters")
    private String isoCurrencyCode;
}
