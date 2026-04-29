package com.batu.plaid_adapter_service.service;

import java.util.List;
import java.util.UUID;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;

public interface RegistryService {

    AccountRegistry findAccount(UUID connectionId, String externalAccountId);

    AccountRegistry createAccount(UUID connectionId, String externalAccountId, UUID accountId);

    AccountRegistry createAccount(UUID connectionId, String externalAccountId, UUID accountId, String fingerprint);

    AccountRegistry updateAccountFingerprint(AccountRegistry accountRegistry, String fingerprint);

    List<AccountRegistry> findAccountsByConnection(UUID connectionId);

    void deleteAccount(UUID accountRegistryId);

    TransactionRegistry findTransaction(UUID connectionId, String externalTransactionId);

    TransactionRegistry createTransaction(AccountRegistry accountRegistry, String externalTransactionId, UUID transactionId);

    void deleteTransaction(UUID transactionRegistryId);
}
