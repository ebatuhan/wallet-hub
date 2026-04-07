package com.batu.shared.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {

    private UUID userId;
    private UUID accountId;
    private String externalId;
    private BigDecimal amount;
    private String isoCurrencyCode;
    private String transactionName;
    private String transactionType;
    private LocalDate date;
    private Boolean pending = Boolean.FALSE;
    private String paymentChannel;
    private String detailedCategoryCode;
    private boolean isActive = true;
}
