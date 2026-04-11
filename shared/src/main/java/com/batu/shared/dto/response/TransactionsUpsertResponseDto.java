package com.batu.shared.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionsUpsertResponseDto {

    private List<PersistedTransactionDto> transactions;
}
