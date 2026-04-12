package com.batu.plaid_adapter_service.service;

import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.command.TransactionSyncCommand;

public interface SyncOutboxService {
    void enqueueAccountCreate(AccountSyncCommand command);

    void enqueueAccountUpdate(AccountSyncCommand command);

    void enqueueTransactionCreate(TransactionSyncCommand command);

    void enqueueTransactionUpdate(TransactionSyncCommand command);
}
