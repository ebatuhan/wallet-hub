package com.batu.transaction_service.service;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.shared.messaging.command.TransactionSyncCommand;

public interface TransactionService {
    CursorResponse<TransactionViewResponseDto> transatcions(Jwt principal,
            String category,
            UUID accountId,
            String cursor,
            int limit);

    TransactionDto getTransactionById(Jwt principial, UUID transactionId);

    void create(TransactionSyncCommand command);

    void update(TransactionSyncCommand command);
}
