package com.batu.plaid_adapter_service.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.shared.dto.response.AccountsUpsertResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateRequestOptions;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncRequestOptions;
import com.plaid.client.model.TransactionsSyncResponse;

@Service
public class PlaidIntegrationServiceImpl implements PlaidIntegrationService {

        @Value("${plaid.webhook.url:}")
        private String webhookUrl;

        private final PlaidClientWrapper plaidClient;
        private final ConnectionService connectionService;
        private final TransactionServiceClient transactionServiceClient;
        private final AccountServiceClient accountServiceClient;

        public PlaidIntegrationServiceImpl(PlaidClientWrapper plaidClient, ConnectionService connectionService,
                        AccountServiceClient accountServiceClient, TransactionServiceClient transactionServiceClient) {
                this.plaidClient = plaidClient;
                this.connectionService = connectionService;
                this.transactionServiceClient = transactionServiceClient;
                this.accountServiceClient = accountServiceClient;
        }

        @Override
        @Retryable(retryFor = {
                        PlaidRetryableException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
        public LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal) {
                var request = new LinkTokenCreateRequest()
                                .userId(principal.getSubject())
                                .clientName("Wallet-Hub")
                                .language("en")
                                .countryCodes(List.of(com.plaid.client.model.CountryCode.US))
                                .products(List.of(Products.TRANSACTIONS))
                                .webhook(webhookUrl);

                LinkTokenCreateResponse response = plaidClient.createLinkToken(request);
                return new LinkTokenResponseDto(response.getLinkToken());
        }

        @Override
        @Retryable(retryFor = {
                        PlaidRetryableException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
        public ExchangeTokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal) {
                var request = new ItemPublicTokenExchangeRequest()
                                .publicToken(exchangeTokenRequestDto.getPublicToken());

                ItemPublicTokenExchangeResponse response = plaidClient.exchangePublicToken(request);

                UUID userId = UUID.fromString(principal.getSubject());

                Connection connection = new Connection(
                                userId,
                                response.getItemId(),
                                response.getAccessToken(),
                                exchangeTokenRequestDto.getInstitutionId(),
                                exchangeTokenRequestDto.getInstitutionName());

                Connection savedConnection = connectionService.create(connection);

                // Initial transactions/sync call
                syncAccountsAndTransactions(connection);

                return new ExchangeTokenResponseDto(
                                savedConnection.getInstitutionId(),
                                savedConnection.getInstitutionName());
        }

        @Override
        @Retryable(retryFor = {
                        PlaidRetryableException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
        public ExchangeTokenResponseDto mockToken(Jwt principal) {
                final String institutionId = "ins_109508";

                SandboxPublicTokenCreateRequest request = new SandboxPublicTokenCreateRequest()
                                .institutionId(institutionId)
                                .initialProducts(List.of(Products.AUTH, Products.TRANSACTIONS))
                                .options(new SandboxPublicTokenCreateRequestOptions()
                                                .webhook(webhookUrl)
                                                .overrideUsername("user_transactions_dynamic")
                                                .overridePassword("user_good"));

                SandboxPublicTokenCreateResponse response = plaidClient.createSandboxToken(request);

                ExchangeTokenRequestDto dto = new ExchangeTokenRequestDto(
                                response.getPublicToken(),
                                Collections.emptyList(),
                                institutionId,
                                "Sandbox Bank");

                return exchangeToken(dto, principal);
        }

        @Override
        @Retryable(retryFor = {
                        PlaidRetryableException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 60000))
        public void syncAccountsAndTransactions(Connection connection) {
                List<Transaction> addedTransactions = new ArrayList<>();
                List<Transaction> modifiedTransactions = new ArrayList<>();
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
                        accounts.addAll(response.getAccounts());
                        cursor = response.getNextCursor();
                        hasMore = response.getHasMore();
                }

                Map<String, UUID> plaidAccountIdToInternalId = upsertAccounts(connection, accounts);

                if (plaidAccountIdToInternalId != null) {
                        upsertTransactions(connection, addedTransactions, modifiedTransactions,
                                        plaidAccountIdToInternalId, cursor);
                }
        }

        private Map<String, UUID> upsertAccounts(Connection connection, Set<AccountBase> accounts) {
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
                System.out.println(
                                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAASDKASKDASDKJASKDJASKDJSKDAJSDK");
                accountsToUpsert.forEach(x -> {
                        System.out.println("---- Account ----");
                        System.out.println("ConnectionId: " + x.getConnectionId());
                        System.out.println("UserId: " + x.getUserId());
                        System.out.println("ExternalId: " + x.getExternalId());
                        System.out.println("Name: " + x.getAccountName());
                        System.out.println("Type: " + x.getAccountType());
                        System.out.println("Subtype: " + x.getAccountSubtype());
                        System.out.println("Mask: " + x.getAccountMask());
                        System.out.println("Current Balance: " + x.getCurrentBalance());
                        System.out.println("Available Balance: " + x.getAvailableBalance());
                        System.out.println("Currency: " + x.getIsoCurrencyCode());
                        System.out.println("Active: " + x.isActive());
                        System.out.println("------------------");
                });

                AccountsUpsertResponseDto response = accountServiceClient
                                .upsertAccountsBatch(new AccountsUpsertRequestDto(accountsToUpsert))
                                .getBody();

                if (response == null) {
                        return null;
                }

                return response.getInsertedAccountsMap();
        }

        private void upsertTransactions(Connection connection,
                        List<Transaction> addedTransactions,
                        List<Transaction> modifiedTransactions,
                        Map<String, UUID> plaidAccountIdToInternalId,
                        String cursor) {
                List<TransactionRequestDto> transactions = Stream.of(addedTransactions, modifiedTransactions)
                                .flatMap(List::stream)
                                .map(tx -> new TransactionRequestDto(
                                                connection.getUserId(),
                                                plaidAccountIdToInternalId.get(tx.getAccountId()),
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

                transactionServiceClient.upsertTransactionsBatch(new TransactionsUpsertRequestDto(transactions));

                connection.setLastCursor(cursor);
                connectionService.updateById(connection.getConnectionId(), connection);
        }
}