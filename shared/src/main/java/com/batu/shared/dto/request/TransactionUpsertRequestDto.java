package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class TransactionUpsertRequestDto {
    @NotNull(message = "Transaction id is required")
    private UUID transactionId;

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Account id is required")
    private UUID accountId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "ISO currency code is required")
    @Size(min = 3, max = 3, message = "ISO currency code must be 3 characters")
    private String isoCurrencyCode;

    @NotBlank(message = "Transaction name is required")
    private String transactionName;

    @NotBlank(message = "Transaction type is required")
    private String transactionType;

    @NotNull(message = "Date is required")
    private LocalDate date;
    private Boolean pending;

    @NotBlank(message = "Payment channel is required")
    private String paymentChannel;

    @NotBlank(message = "Detailed category code is required")
    private String detailedCategoryCode;
    private boolean active;
}
