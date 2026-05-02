package com.batu.transaction_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.service.input.RecordTransactionInput;

public interface TransactionService {
    CursorResponse<TransactionViewResponseDto> transactions(Jwt principal,
            String category,
            UUID accountId,
            String cursor,
            int limit);

    CursorResponse<TransactionViewResponseDto> transactions(UUID userId,
            String category,
            UUID accountId,
            String cursor,
            int limit);

    TransactionDto getTransactionById(Jwt principal, UUID transactionId);

    TransactionDto getTransactionById(UUID userId, UUID transactionId);

    Optional<Transaction> recordTransaction(RecordTransactionInput input);

    List<Transaction> deactivateByAccountId(UUID accountId, long version);
}
