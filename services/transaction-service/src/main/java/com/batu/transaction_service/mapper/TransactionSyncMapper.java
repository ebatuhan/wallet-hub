package com.batu.transaction_service.mapper;

import org.springframework.stereotype.Component;

import com.batu.shared.dto.response.TransactionDetailedCategoryDto;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.transaction_service.entity.Transaction;

@Component
public class TransactionSyncMapper {
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
