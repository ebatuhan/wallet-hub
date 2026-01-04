package com.batu.plaid_adapter_service.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.service.SyncService;
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

    public SyncServiceImpl(PlaidApi plaidClient) {
        this.plaidClient = plaidClient;
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
        while (hasMore) {

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
                }

                else {
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
            }

            catch (IOException ex) {
                throw new PlaidClientException("Plaid API refused the connection.", HttpStatus.SERVICE_UNAVAILABLE);
            }

        }
    }
}