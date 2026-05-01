package com.batu.plaid_adapter_service.service;

import java.util.UUID;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;

public interface TransactionRegistryService {

    TransactionRegistry findTransaction(UUID connectionId, String externalTransactionId);

    TransactionRegistry upsertTransaction(AccountRegistry accountRegistry, String externalTransactionId,
            UUID transactionId);
    void deleteTransaction(UUID transactionRegistryId);
}
