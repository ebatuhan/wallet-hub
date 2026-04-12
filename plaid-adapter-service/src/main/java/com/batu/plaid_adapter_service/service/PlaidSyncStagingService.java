package com.batu.plaid_adapter_service.service;

import java.util.List;

import com.batu.plaid_adapter_service.entity.Connection;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;

public interface PlaidSyncStagingService {
    void stageAccounts(Connection connection, List<AccountBase> accounts);

    void stageTransactions(Connection connection, List<Transaction> addedTransactions,
            List<Transaction> modifiedTransactions,
            List<RemovedTransaction> removedTransactions,
            String cursor);

    void deactivateConnectionData(Connection connection);
}
