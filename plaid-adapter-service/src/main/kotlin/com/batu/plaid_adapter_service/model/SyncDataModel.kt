package com.batu.model

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;
import com.batu.plaid_adapter_service.entity.Connection


data class SyncDataModel(
    val accounts : Set<AccountBase> = emptySet(),
    val addedTransactions : List<Transaction> = emptyList(),
    val modifiedTransactions : List<Transaction> = emptyList(),
    val removedTransactions : List<RemovedTransaction> = emptyList(),
    val connection : Connection
)