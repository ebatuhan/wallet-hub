package com.batu.model;

import com.batu.plaid_adapter_service.entity.Connection;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SyncDataModel {

    private Set<AccountBase> accounts;
    private List<Transaction> addedTransactions;
    private List<Transaction> modifiedTransactions;
    private List<RemovedTransaction> removedTransactions;
    private Connection connection;
}
