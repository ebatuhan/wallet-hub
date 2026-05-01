package com.batu.transaction_service.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.response.TransactionDetailedCategoryDto;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.messaging.event.TransactionPersistedEvent;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;

@Component
public class TransactionSyncMapper {

    public Transaction toEntity(TransactionRequestDto request, TransactionDetailedCategory detailedCategory) {
        return new Transaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAccountId(),
                request.getAmount(),
                request.getIsoCurrencyCode(),
                request.getTransactionName(),
                request.getTransactionType(),
                request.getDate(),
                request.getPending(),
                request.getPaymentChannel(),
                detailedCategory,
                request.isActive(),
                request.getSyncVersion());
    }

    public TransactionPersistedEvent toPersistedEvent(Transaction transaction) {
        return new TransactionPersistedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "transaction-service",
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getIsoCurrencyCode(),
                transaction.getTransactionName(),
                transaction.getTransactionType(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPaymentChannel(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode(),
                transaction.isActive(),
                transaction.getSyncVersion());
    }

    public TransactionDto toDto(Transaction transaction) {
        TransactionPrimaryCategoryDto primaryCategoryDto = new TransactionPrimaryCategoryDto(
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getDisplayName(),
                transaction.getDetailedCategory().getTransactionPrimaryCategory().getIconUrl());

        TransactionDetailedCategoryDto detailedCategoryDto = new TransactionDetailedCategoryDto(
                transaction.getDetailedCategory().getTransactionDetailedCategoryId(),
                transaction.getDetailedCategory().getDisplayName(),
                transaction.getDetailedCategory().getCategoryCode(),
                primaryCategoryDto);

        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getIsoCurrencyCode(),
                transaction.getTransactionName(),
                transaction.getTransactionType(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPaymentChannel(),
                detailedCategoryDto,
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
