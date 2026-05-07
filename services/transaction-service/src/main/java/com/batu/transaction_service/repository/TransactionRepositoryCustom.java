package com.batu.transaction_service.repository;

import java.util.UUID;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.transaction_service.entity.Transaction;

public interface TransactionRepositoryCustom {
    Transaction upsertTransaction(TransactionUpsertRequestDto request, UUID detailedCategoryId);
}
