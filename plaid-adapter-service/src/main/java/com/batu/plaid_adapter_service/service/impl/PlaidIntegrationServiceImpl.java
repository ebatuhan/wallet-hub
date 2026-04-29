package com.batu.plaid_adapter_service.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.annotation.Observed;

import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.TransactionRegistry;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.service.RegistryService;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountsGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
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

    @Value("${plaid.webhook.url:}")
    private String webhookUrl;

    private final PlaidClientWrapper plaidClient;
    private final ConnectionService connectionService;
    private final RegistryService registryService;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final PlaidRequestMapper plaidRequestMapper;
    private final PlaidDuplicateConnectionService duplicateConnectionService;

    public PlaidIntegrationServiceImpl(PlaidClientWrapper plaidClient,
            ConnectionService connectionService,
            RegistryService registryService,
            AccountServiceClient accountServiceClient,
            TransactionServiceClient transactionServiceClient,
            PlaidRequestMapper plaidRequestMapper,
            PlaidDuplicateConnectionService duplicateConnectionService) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
        this.registryService = registryService;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.plaidRequestMapper = plaidRequestMapper;
        this.duplicateConnectionService = duplicateConnectionService;
    }

    @Override
    @Observed(name = "plaid.create-link-token", contextualName = "plaid create link token")
    @Retryable(retryFor = PlaidRetryableException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, UUID userId) {
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(new LinkTokenCreateRequestUser().clientUserId(userId.toString()))
                .clientName("Wallet-Hub")
                .language("en")
                .countryCodes(List.of(com.plaid.client.model.CountryCode.US))
                .products(List.of(Products.TRANSACTIONS))
                .webhook(webhookUrl);

        LinkTokenCreateResponse response = plaidClient.createLinkToken(request);

        return new LinkTokenResponseDto(response.getLinkToken());
    }

    @Override
    @Transactional
    @Observed(name = "plaid.exchange-token", contextualName = "plaid exchange token")
    @Retryable(retryFor = PlaidRetryableException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public ExchangeTokenResponseDto exchangeLinkToken(ExchangeTokenRequestDto exchangeTokenRequestDto, UUID userId) {
        duplicateConnectionService.validateNotDuplicate(exchangeTokenRequestDto, userId);

        ItemPublicTokenExchangeResponse response = plaidClient.exchangePublicToken(
                new ItemPublicTokenExchangeRequest().publicToken(exchangeTokenRequestDto.getPublicToken()));

        Connection connection = new Connection(
                userId,
                response.getItemId(),
                response.getAccessToken(),
                exchangeTokenRequestDto.getInstitutionId(),
                exchangeTokenRequestDto.getInstitutionName());

        Connection savedConnection = connectionService.create(connection);
        syncConnection(savedConnection.getConnectionId());

        return new ExchangeTokenResponseDto(
                savedConnection.getConnectionId(),
                savedConnection.getInstitutionId(),
                savedConnection.getInstitutionName());
    }

    @Override
    @Observed(name = "plaid.mock-token", contextualName = "plaid mock token")
    @Retryable(retryFor = PlaidRetryableException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public ExchangeTokenResponseDto mockToken(UUID userId) {
        String institutionId = "ins_109508";

        SandboxPublicTokenCreateResponse response = plaidClient.createSandboxToken(
                new SandboxPublicTokenCreateRequest()
                        .institutionId(institutionId)
                        .initialProducts(List.of(Products.TRANSACTIONS))
                        .options(new SandboxPublicTokenCreateRequestOptions()
                                .webhook(webhookUrl)
                                .overrideUsername("user_good")
                                .overridePassword("pass_good")));

        return exchangeLinkToken(
                new ExchangeTokenRequestDto(response.getPublicToken(), Collections.emptyList(), Collections.emptyList(), institutionId, "Sandbox Bank"),
                userId);
    }

    @Override
    @Observed(name = "plaid.sync-connection", contextualName = "plaid sync connection")
    @Retryable(retryFor = PlaidRetryableException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 60000))
    public void syncConnection(UUID connectionId) {
        Connection connection = connectionService.startSync(connectionId);
        if (connection == null) {
            return;
        }

        try {
            Map<String, AccountRegistry> accountCache = new HashMap<>();
            Map<String, TransactionRegistry> transactionCache = new HashMap<>();

            syncAccounts(connection, accountCache);

            String cursor = connection.getLastCursor();
            boolean hasMore = true;

            while (hasMore) {
                TransactionsSyncResponse response = plaidClient.syncTransactions(
                        new TransactionsSyncRequest()
                                .accessToken(connection.getAccessToken())
                                .cursor(cursor)
                                .options(new TransactionsSyncRequestOptions().includePersonalFinanceCategory(true)));

                syncTransactions(connection, response.getAdded(), accountCache, transactionCache);
                syncTransactions(connection, response.getModified(), accountCache, transactionCache);
                deactivateRemovedTransactions(connection, response.getRemoved(), transactionCache);

                cursor = response.getNextCursor();
                hasMore = response.getHasMore();
            }

            connectionService.completeSync(connection.getConnectionId(), cursor);
        } catch (RuntimeException ex) {
            connectionService.releaseSync(connectionId);
            throw ex;
        }
    }

    @Override
    @Transactional
    @Observed(name = "plaid.remove-connection", contextualName = "plaid remove connection")
    public void removeConnection(UUID connectionId, String reason) {
        connectionService.markRemoving(connectionId, reason);

        for (AccountRegistry accountRegistry : registryService.findAccountsByConnection(connectionId)) {
            transactionServiceClient.deactivateByAccountId(accountRegistry.getAccountId());
            accountServiceClient.deactivate(accountRegistry.getAccountId());
        }

        connectionService.markRemoved(connectionId, reason);
    }

    private void syncAccounts(Connection connection, Map<String, AccountRegistry> accountCache) {
        AccountsGetResponse response = plaidClient.accountsGet(
                new AccountsGetRequest().accessToken(connection.getAccessToken()));

        for (AccountBase account : response.getAccounts()) {
            AccountRegistry registry = accountCache.get(account.getAccountId());
            if (registry == null) {
                registry = registryService.findAccount(connection.getConnectionId(), account.getAccountId());
                if (registry != null) {
                    accountCache.put(account.getAccountId(), registry);
                }
            }

            if (registry == null) {
                registry = registryService.createAccount(
                        connection.getConnectionId(),
                        account.getAccountId(),
                        UUID.randomUUID(),
                        duplicateConnectionService.createFingerprint(account.getName(), account.getMask()));
                try {
                    accountServiceClient.create(
                            plaidRequestMapper.toAccountRequest(connection, registry.getAccountId(), account));
                } catch (RuntimeException ex) {
                    registryService.deleteAccount(registry.getAccountRegistryId());
                    throw ex;
                }
                accountCache.put(account.getAccountId(), registry);
                continue;
            }

            String fingerprint = duplicateConnectionService.createFingerprint(account.getName(), account.getMask());
            if (!fingerprint.equals(registry.getFingerprint())) {
                registry = registryService.updateAccountFingerprint(registry, fingerprint);
            }

            accountServiceClient.update(
                    registry.getAccountId(),
                    plaidRequestMapper.toAccountRequest(connection, registry.getAccountId(), account));
        }
    }

    private void syncTransactions(Connection connection,
            List<Transaction> transactions,
            Map<String, AccountRegistry> accountCache,
            Map<String, TransactionRegistry> transactionCache) {
        for (Transaction transaction : transactions) {
            AccountRegistry accountRegistry = accountCache.get(transaction.getAccountId());
            if (accountRegistry == null) {
                accountRegistry = registryService.findAccount(connection.getConnectionId(), transaction.getAccountId());
                if (accountRegistry == null) {
                    continue;
                }
                accountCache.put(transaction.getAccountId(), accountRegistry);
            }

            deactivatePendingTransaction(connection, transaction, transactionCache);

            TransactionRegistry registry = transactionCache.get(transaction.getTransactionId());
            if (registry == null) {
                registry = registryService.findTransaction(connection.getConnectionId(), transaction.getTransactionId());
                if (registry != null) {
                    transactionCache.put(transaction.getTransactionId(), registry);
                }
            }

            if (registry == null) {
                registry = registryService.createTransaction(accountRegistry, transaction.getTransactionId(), UUID.randomUUID());
                try {
                    transactionServiceClient.create(
                            plaidRequestMapper.toTransactionRequest(
                                    connection,
                                    registry.getTransactionId(),
                                    accountRegistry.getAccountId(),
                                    transaction));
                } catch (RuntimeException ex) {
                    registryService.deleteTransaction(registry.getTransactionRegistryId());
                    throw ex;
                }
                transactionCache.put(transaction.getTransactionId(), registry);
                continue;
            }

            transactionServiceClient.update(
                    registry.getTransactionId(),
                    plaidRequestMapper.toTransactionRequest(
                            connection,
                            registry.getTransactionId(),
                            accountRegistry.getAccountId(),
                            transaction));
        }
    }

    private void deactivateRemovedTransactions(Connection connection,
            List<RemovedTransaction> removedTransactions,
            Map<String, TransactionRegistry> transactionCache) {
        for (RemovedTransaction removedTransaction : removedTransactions) {
            TransactionRegistry registry = transactionCache.get(removedTransaction.getTransactionId());
            if (registry == null) {
                registry = registryService.findTransaction(connection.getConnectionId(), removedTransaction.getTransactionId());
                if (registry != null) {
                    transactionCache.put(removedTransaction.getTransactionId(), registry);
                }
            }

            if (registry != null) {
                transactionServiceClient.update(
                        registry.getTransactionId(),
                        plaidRequestMapper.toDeactivateTransactionRequest(connection, registry.getTransactionId()));
            }
        }
    }

    private void deactivatePendingTransaction(Connection connection,
            Transaction transaction,
            Map<String, TransactionRegistry> transactionCache) {
        if (transaction.getPendingTransactionId() == null || transaction.getPendingTransactionId().isBlank()) {
            return;
        }

        TransactionRegistry pendingRegistry = transactionCache.get(transaction.getPendingTransactionId());
        if (pendingRegistry == null) {
            pendingRegistry = registryService.findTransaction(connection.getConnectionId(), transaction.getPendingTransactionId());
            if (pendingRegistry != null) {
                transactionCache.put(transaction.getPendingTransactionId(), pendingRegistry);
            }
        }

        if (pendingRegistry != null) {
            transactionServiceClient.update(
                    pendingRegistry.getTransactionId(),
                    plaidRequestMapper.toDeactivateTransactionRequest(connection, pendingRegistry.getTransactionId()));
        }
    }

}
