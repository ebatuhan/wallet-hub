package com.batu.plaid_adapter_service.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.plaid_adapter_service.entity.AccountSyncLookup;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.TransactionSyncLookup;
import com.batu.plaid_adapter_service.mapper.PlaidSyncCommandMapper;
import com.batu.plaid_adapter_service.repository.AccountSyncLookupRepository;
import com.batu.plaid_adapter_service.repository.TransactionSyncLookupRepository;
import com.batu.plaid_adapter_service.exception.SyncStateException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidSyncStagingService;
import com.batu.plaid_adapter_service.service.SyncOutboxService;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;

@Service
public class PlaidSyncStagingServiceImpl implements PlaidSyncStagingService {

    private final AccountSyncLookupRepository accountSyncLookupRepository;
    private final TransactionSyncLookupRepository transactionSyncLookupRepository;
    private final SyncOutboxService syncOutboxService;
    private final ConnectionService connectionService;
    private final PlaidSyncCommandMapper plaidSyncCommandMapper;

    public PlaidSyncStagingServiceImpl(AccountSyncLookupRepository accountSyncLookupRepository,
            TransactionSyncLookupRepository transactionSyncLookupRepository,
            SyncOutboxService syncOutboxService,
            ConnectionService connectionService,
            PlaidSyncCommandMapper plaidSyncCommandMapper) {
        this.accountSyncLookupRepository = accountSyncLookupRepository;
        this.transactionSyncLookupRepository = transactionSyncLookupRepository;
        this.syncOutboxService = syncOutboxService;
        this.connectionService = connectionService;
        this.plaidSyncCommandMapper = plaidSyncCommandMapper;
    }

    @Override
    @Transactional
    public void stageAccounts(Connection connection, List<AccountBase> accounts) {
        Map<String, AccountSyncLookup> accountLookupsByPlaidId = loadAccountLookups(accounts);

        for (AccountBase account : accounts) {
            AccountSyncLookup lookup = accountLookupsByPlaidId.get(account.getAccountId());

            if (lookup == null) {
                UUID accountId = UUID.randomUUID();
                lookup = accountSyncLookupRepository.save(new AccountSyncLookup(
                        connection.getConnectionId(),
                        account.getAccountId(),
                        accountId));
                accountLookupsByPlaidId.put(account.getAccountId(), lookup);

                syncOutboxService.enqueueAccountCreate(
                        plaidSyncCommandMapper.toAccountCommand(connection, accountId, account));
                continue;
            }

            syncOutboxService.enqueueAccountUpdate(
                    plaidSyncCommandMapper.toAccountCommand(connection, lookup.getAccountId(), account));
        }
    }

    @Override
    @Transactional
    public void stageTransactions(Connection connection, List<Transaction> addedTransactions,
            List<Transaction> modifiedTransactions,
            List<RemovedTransaction> removedTransactions,
            String cursor) {
        List<Transaction> transactions = new ArrayList<>(addedTransactions.size() + modifiedTransactions.size());
        transactions.addAll(addedTransactions);
        transactions.addAll(modifiedTransactions);

        Map<String, UUID> accountIdsByPlaidId = loadAccountIdsByPlaidId(transactions);
        Map<String, TransactionSyncLookup> transactionLookupsByPlaidId = loadTransactionLookups(transactions);

        for (Transaction transaction : transactions) {
            UUID accountId = requireAccountId(accountIdsByPlaidId, transaction.getAccountId());
            TransactionSyncLookup lookup = transactionLookupsByPlaidId.get(transaction.getTransactionId());

            if (lookup == null) {
                UUID transactionId = UUID.randomUUID();
                lookup = transactionSyncLookupRepository.save(new TransactionSyncLookup(
                        connection.getConnectionId(),
                        transaction.getTransactionId(),
                        transactionId));
                transactionLookupsByPlaidId.put(transaction.getTransactionId(), lookup);

                syncOutboxService.enqueueTransactionCreate(
                        plaidSyncCommandMapper.toTransactionCommand(connection, transactionId, accountId, transaction));
                continue;
            }

            syncOutboxService.enqueueTransactionUpdate(
                    plaidSyncCommandMapper.toTransactionCommand(connection, lookup.getTransactionId(), accountId,
                            transaction));
        }

        if (!removedTransactions.isEmpty()) {
            Map<String, TransactionSyncLookup> removedLookupMap = loadRemovedTransactionLookups(removedTransactions);

            for (RemovedTransaction removedTransaction : removedTransactions) {
                TransactionSyncLookup lookup = removedLookupMap.get(removedTransaction.getTransactionId());

                if (lookup == null) {
                    continue;
                }

                syncOutboxService.enqueueTransactionUpdate(
                        plaidSyncCommandMapper.toDeactivateTransactionCommand(connection, lookup.getTransactionId()));
            }
        }

        connectionService.completeSync(connection.getConnectionId(), cursor);
    }

    @Override
    @Transactional
    public void deactivateConnectionData(Connection connection) {
        for (AccountSyncLookup accountLookup : accountSyncLookupRepository.findByConnectionId(connection.getConnectionId())) {
            syncOutboxService.enqueueAccountUpdate(
                    plaidSyncCommandMapper.toDeactivateAccountCommand(connection, accountLookup.getAccountId()));
        }

        for (TransactionSyncLookup transactionLookup : transactionSyncLookupRepository
                .findByConnectionId(connection.getConnectionId())) {
            syncOutboxService.enqueueTransactionUpdate(
                    plaidSyncCommandMapper.toDeactivateTransactionCommand(connection, transactionLookup.getTransactionId()));
        }
    }

    private Map<String, AccountSyncLookup> loadAccountLookups(List<AccountBase> accounts) {
        java.util.Set<String> plaidAccountIds = new java.util.HashSet<>();
        Map<String, AccountSyncLookup> accountLookupsByPlaidId = new HashMap<>();

        for (AccountBase account : accounts) {
            plaidAccountIds.add(account.getAccountId());
        }

        for (AccountSyncLookup lookup : accountSyncLookupRepository.findByPlaidAccountIdIn(plaidAccountIds)) {
            accountLookupsByPlaidId.put(lookup.getPlaidAccountId(), lookup);
        }

        return accountLookupsByPlaidId;
    }

    private Map<String, UUID> loadAccountIdsByPlaidId(List<Transaction> transactions) {
        java.util.Set<String> plaidAccountIds = new java.util.HashSet<>();

        for (Transaction transaction : transactions) {
            plaidAccountIds.add(transaction.getAccountId());
        }

        Map<String, UUID> accountIdsByPlaidId = new HashMap<>();

        for (AccountSyncLookup lookup : accountSyncLookupRepository.findByPlaidAccountIdIn(plaidAccountIds)) {
            accountIdsByPlaidId.put(lookup.getPlaidAccountId(), lookup.getAccountId());
        }

        return accountIdsByPlaidId;
    }

    private Map<String, TransactionSyncLookup> loadTransactionLookups(List<Transaction> transactions) {
        java.util.Set<String> plaidTransactionIds = new java.util.HashSet<>();
        Map<String, TransactionSyncLookup> transactionLookupsByPlaidId = new HashMap<>();

        for (Transaction transaction : transactions) {
            plaidTransactionIds.add(transaction.getTransactionId());
        }

        for (TransactionSyncLookup lookup : transactionSyncLookupRepository.findByPlaidTransactionIdIn(plaidTransactionIds)) {
            transactionLookupsByPlaidId.put(lookup.getPlaidTransactionId(), lookup);
        }

        return transactionLookupsByPlaidId;
    }

    private Map<String, TransactionSyncLookup> loadRemovedTransactionLookups(List<RemovedTransaction> removedTransactions) {
        java.util.Set<String> plaidTransactionIds = new java.util.HashSet<>();
        Map<String, TransactionSyncLookup> transactionLookupsByPlaidId = new HashMap<>();

        for (RemovedTransaction removedTransaction : removedTransactions) {
            plaidTransactionIds.add(removedTransaction.getTransactionId());
        }

        for (TransactionSyncLookup lookup : transactionSyncLookupRepository.findByPlaidTransactionIdIn(plaidTransactionIds)) {
            transactionLookupsByPlaidId.put(lookup.getPlaidTransactionId(), lookup);
        }

        return transactionLookupsByPlaidId;
    }

    private UUID requireAccountId(Map<String, UUID> accountIdsByPlaidId, String plaidAccountId) {
        UUID accountId = accountIdsByPlaidId.get(plaidAccountId);

        if (accountId == null) {
            throw new SyncStateException("Missing account lookup for Plaid account " + plaidAccountId);
        }

        return accountId;
    }
}
