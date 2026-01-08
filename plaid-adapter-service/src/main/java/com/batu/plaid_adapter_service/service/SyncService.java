package com.batu.plaid_adapter_service.service;


import com.batu.model.SyncDataModel;
import com.batu.plaid_adapter_service.entity.Connection;

public interface SyncService {
    SyncDataModel SyncTransactionsAndAccounts(Connection connection);
}