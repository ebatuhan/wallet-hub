package com.batu.plaid_adapter_service.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.SyncService;
import com.batu.shared.dto.AccountRequestDto;
import com.batu.shared.dto.AccountsUpsertRequestDto;
import com.batu.shared.dto.AccountsUpsertResponseDto;
import com.batu.shared.dto.TransactionRequestDto;
import com.batu.shared.dto.TransactionsUpsertRequestDto;
import com.google.gson.Gson;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.PlaidError;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncRequestOptions;
import com.plaid.client.request.PlaidApi;

@Service //TODELETE
public class SyncServiceImpl implements SyncService {

    private final PlaidApi plaidClient;
    private final ConnectionService connectionService;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    public SyncServiceImpl(PlaidApi plaidClient, AccountServiceClient accountServiceClient, ConnectionService connectionService, TransactionServiceClient transactionServiceClient) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
    }

    @Override
    public void SyncTransactionsAndAccounts(Connection connection) {

        List<Transaction> addedTransactions = new ArrayList<>();
        List<Transaction> modifiedTransactions = new ArrayList<>();
        List<RemovedTransaction> removedTransactions = new ArrayList<>();
        Set<AccountBase> accounts = new HashSet<>();

        String lastCursor = connection.getLastCursor();

        var transactionsSyncRequestOptions = new TransactionsSyncRequestOptions()
                .includePersonalFinanceCategory(true);

        boolean hasMore = true;
        int iteration = 0;
        while (hasMore) {
            iteration++;

            var request = new TransactionsSyncRequest()
                    .accessToken(connection.getAccessToken())
                    .options(transactionsSyncRequestOptions)
                    .cursor(lastCursor);

            try {

                var response = plaidClient.transactionsSync(request).execute();
                var body = response.body();

                if (body != null && response.isSuccessful()) {
                    addedTransactions.addAll(body.getAdded());
                    modifiedTransactions.addAll(body.getModified());
                    removedTransactions.addAll(body.getRemoved());
                    accounts.addAll(body.getAccounts());

                    lastCursor = body.getNextCursor();
                    hasMore = body.getHasMore();
                } else {

                    var errorBody = response.errorBody();
                    if (errorBody != null) {
                        Gson gson = new Gson();
                        var plaidError = gson.fromJson(errorBody.string(), PlaidError.class);

                        switch (plaidError.getErrorCode()) {
                            case "TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION":

                                lastCursor = connection.getLastCursor();
                                hasMore = true;
                                addedTransactions.clear();
                                modifiedTransactions.clear();
                                removedTransactions.clear();
                                accounts.clear();
                                continue;
                            default:
                                String displayMessage = plaidError.getDisplayMessage() != null
                                        ? plaidError.getDisplayMessage()
                                        : "An error occurred in Plaid";
                                throw new PlaidClientException(displayMessage, HttpStatus.BAD_GATEWAY);
                        }
                    }
                    throw new PlaidClientException("Plaid sent empty error", HttpStatus.BAD_GATEWAY);
                }
            } catch (IOException ex) {

                throw new PlaidClientException("Plaid API refused the connection.", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        List<AccountRequestDto> accountsToUpsert = accounts.stream()
                .map(acc -> new AccountRequestDto(connection.getConnectionId(),
                        connection.getUserId(),
                        acc.getAccountId(),
                        acc.getName(),
                        acc.getType().getValue(),
                        acc.getSubtype().getValue(),
                        acc.getMask(),
                        BigDecimal.valueOf(acc.getBalances().getCurrent()),
                        BigDecimal.valueOf(acc.getBalances().getAvailable()),
                        acc.getBalances().getIsoCurrencyCode(),
                        true))
                .collect(Collectors.toList());

        var accountsUpsertRequest = new AccountsUpsertRequestDto(accountsToUpsert);

        AccountsUpsertResponseDto accountsUpsertResponse = accountServiceClient
                .upsertAccountsBatch(accountsUpsertRequest).getBody();

        if (accountsUpsertResponse != null) {

            List<TransactionRequestDto> transactions = Stream.of(addedTransactions, modifiedTransactions)
                    .flatMap(List::stream)
                    .map(tx -> new TransactionRequestDto(
                            connection.getUserId(),
                            accountsUpsertResponse.getInsertedAccountsMap().get(tx.getAccountId()),
                            tx.getTransactionId(),
                            BigDecimal.valueOf(tx.getAmount()),
                            tx.getIsoCurrencyCode(),
                            tx.getName(),
                            tx.getTransactionType().getValue(),
                            tx.getDate(),
                            tx.getPending(),
                            tx.getPaymentChannel().getValue(),
                            tx.getPersonalFinanceCategory().getDetailed(),
                            true))
                    .collect(Collectors.toList());

            var transactionsUpsertRequest = new TransactionsUpsertRequestDto(transactions);
            var upsertTransactions = transactionServiceClient.upsertTransactionsBatch(transactionsUpsertRequest);

            connection.setLastCursor(lastCursor);
            connectionService.updateById(connection.getConnectionId(), connection);
        }
    }

    }
