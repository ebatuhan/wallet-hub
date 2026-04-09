

package com.batu.plaid_adapter_service.service;


import com.batu.plaid_adapter_service.entity.Connection;

public interface FetchService {

    void fetchAccounts(Connection connection);
    void SyncTransactionsAndAccounts(Connection connection);
}
