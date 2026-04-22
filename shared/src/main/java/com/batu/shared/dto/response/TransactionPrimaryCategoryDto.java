package com.batu.shared.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionPrimaryCategoryDto {

    private UUID transactionPrimaryCategoryId;
    private String categoryCode;
    private String displayName;
    private String iconUrl;
}
