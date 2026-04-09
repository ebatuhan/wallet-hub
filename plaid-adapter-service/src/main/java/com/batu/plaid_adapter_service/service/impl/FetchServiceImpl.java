
/*package com.batu.plaid_adapter_service.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.batu.model.SyncDataModel;
import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.SyncService;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.shared.dto.response.AccountsUpsertResponseDto;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncRequestOptions;
import com.plaid.client.model.TransactionsSyncResponse;

@Service
public class SyncServiceImpl implements SyncService {

    private final PlaidClientWrapper plaidClient;
    private final ConnectionService connectionService;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    public SyncServiceImpl(PlaidClientWrapper plaidClient,
            AccountServiceClient accountServiceClient,
            ConnectionService connectionService,
            TransactionServiceClient transactionServiceClient) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
    }

    @Override
    @Retryable(retryFor = {
            PlaidRetryableException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 60000))
    public void SyncTransactionsAndAccounts(Connection connection) {
        List<Transaction> addedTransactions = new ArrayList<>();
        List<Transaction> modifiedTransactions = new ArrayList<>();
        List<RemovedTransaction> removedTransactions = new ArrayList<>();
        Set<AccountBase> accounts = new HashSet<>();

        String cursor = connection.getLastCursor();
        var options = new TransactionsSyncRequestOptions()
                .includePersonalFinanceCategory(true);

        boolean hasMore = true;

        while (hasMore) {
            var request = new TransactionsSyncRequest()
                    .accessToken(connection.getAccessToken())
                    .options(options)
                    .cursor(cursor);

            TransactionsSyncResponse response = plaidClient.syncTransactions(request);

            addedTransactions.addAll(response.getAdded());
            modifiedTransactions.addAll(response.getModified());
            removedTransactions.addAll(response.getRemoved());
            accounts.addAll(response.getAccounts());
            cursor = response.getNextCursor();
            hasMore = response.getHasMore();
        }
    }

    private void upsertAccountsAndTransactions(Connection connection,
            List<Transaction> addedTransactions,
            List<Transaction> modifiedTransactions,
            Set<AccountBase> accounts,
            String cursor) {
        List<AccountRequestDto> accountsToUpsert = accounts.stream()
                .map(acc -> new AccountRequestDto(
                        connection.getConnectionId(),
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
                .upsertAccountsBatch(accountsUpsertRequest)
                .getBody();

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
            transactionServiceClient.upsertTransactionsBatch(transactionsUpsertRequest);

            connection.setLastCursor(cursor);
            connectionService.updateById(connection.getConnectionId(), connection);
        }
    }
}
 */
