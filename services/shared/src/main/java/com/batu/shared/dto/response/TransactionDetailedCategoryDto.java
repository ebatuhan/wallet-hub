package com.batu.shared.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionDetailedCategoryDto {

    private UUID transactionDetailedCategoryId;
    private String displayName;
    private String detailedCode;
    private TransactionPrimaryCategoryDto primaryCategory;
}
