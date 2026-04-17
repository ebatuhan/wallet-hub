package com.batu.transaction_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.request.AccountIdsRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

public interface TransactionService {
    CursorResponse<TransactionViewResponseDto> transatcions(Jwt principal,
            String category,
            UUID accountId,
            String cursor,
            int limit);

    TransactionDto getTransactionById(Jwt principial, UUID transactionId);

    void saveBatch(TransactionsUpsertRequestDto request);

    void deactivateByAccountIds(AccountIdsRequestDto request);
}
