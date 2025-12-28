package com.batu.plaid_adapter_service.service;


import com.batu.plaid_adapter_service.entity.Connection;

public interface SyncService {
    void SyncTransactionsAndAccounts(Connection connection);
}