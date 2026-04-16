package com.batu.plaid_adapter_service.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.service.PlaidSyncStagingService;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.plaid.client.model.AccountsBalanceGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateRequestOptions;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.model.Transaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncRequestOptions;
import com.plaid.client.model.TransactionsSyncResponse;

@Service
public class PlaidIntegrationServiceImpl implements PlaidIntegrationService {

        private static final Duration SYNC_LOCK_STALE_AFTER = Duration.ofMinutes(5);

        @Value("${plaid.webhook.url:}")
        private String webhookUrl;

        private final PlaidClientWrapper plaidClient;
        private final ConnectionService connectionService;
        private final PlaidSyncStagingService plaidSyncStagingService;

        public PlaidIntegrationServiceImpl(PlaidClientWrapper plaidClient, ConnectionService connectionService,
                        PlaidSyncStagingService plaidSyncStagingService) {
                this.plaidClient = plaidClient;
                this.connectionService = connectionService;
                this.plaidSyncStagingService = plaidSyncStagingService;
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

                syncAccountsAndTransactions(savedConnection);

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
                                .initialProducts(List.of(Products.TRANSACTIONS))
                                .options(new SandboxPublicTokenCreateRequestOptions()
                                                .webhook(webhookUrl)
                                                .overrideUsername("user_good") //user_transactions_dynamic
                                                .overridePassword("pass_good")); // user_good

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
                if (!connectionService.claimSync(connection.getConnectionId(), SYNC_LOCK_STALE_AFTER)) {
                        return;
                }

                Connection currentConnection = connectionService.readById(connection.getConnectionId());

                try {
                        syncAccounts(currentConnection);
                        syncTransactions(currentConnection);
                } catch (RuntimeException ex) {
                        connectionService.releaseSync(currentConnection.getConnectionId());
                        throw ex;
                }
        }

        private void syncAccounts(Connection connection) {
                AccountsBalanceGetRequest request = new AccountsBalanceGetRequest()
                                .accessToken(connection.getAccessToken());

                AccountsGetResponse response = plaidClient.accountsBalanceGet(request);
                plaidSyncStagingService.stageAccounts(connection, response.getAccounts());
        }

        private void syncTransactions(Connection connection) {
                List<Transaction> addedTransactions = new ArrayList<>();
                List<Transaction> modifiedTransactions = new ArrayList<>();
                List<RemovedTransaction> removedTransactions = new ArrayList<>();
                String cursor = connection.getLastCursor();
                boolean hasMore = true;

                while (hasMore) {
                        TransactionsSyncRequest request = new TransactionsSyncRequest()
                                        .accessToken(connection.getAccessToken())
                                        .cursor(cursor)
                                        .options(new TransactionsSyncRequestOptions()
                                                        .includePersonalFinanceCategory(true));

                        TransactionsSyncResponse response = plaidClient.syncTransactions(request);
                        addedTransactions.addAll(response.getAdded());
                        modifiedTransactions.addAll(response.getModified());
                        removedTransactions.addAll(response.getRemoved());
                        cursor = response.getNextCursor();
                        hasMore = response.getHasMore();
                }

                plaidSyncStagingService.stageTransactions(
                                connection,
                                addedTransactions,
                                modifiedTransactions,
                                removedTransactions,
                                cursor);
        }
}
