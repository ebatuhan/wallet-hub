package com.batu.plaid_adapter_service.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.annotation.Observed;

import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.DuplicateConnectionException;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.messaging.PlaidOutbox;
import com.batu.plaid_adapter_service.service.AccountRegistryService;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;
import com.batu.plaid_adapter_service.util.StringHasher;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.ConnectionRemoved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.ItemRemoveRequest;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
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
    private final AccountRegistryService accountRegistryService;
    private final PlaidRequestMapper plaidRequestMapper;
    private final PlaidOutbox plaidOutbox;
    private final DeterministicIdGenerator deterministicIdGenerator;
    private final StringHasher stringHasher;

    public PlaidIntegrationServiceImpl(PlaidClientWrapper plaidClient,
            ConnectionService connectionService,
            AccountRegistryService accountRegistryService,
            PlaidRequestMapper plaidRequestMapper,
            PlaidOutbox plaidOutbox,
            DeterministicIdGenerator deterministicIdGenerator,
            StringHasher stringHasher) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
        this.accountRegistryService = accountRegistryService;
        this.plaidRequestMapper = plaidRequestMapper;
        this.plaidOutbox = plaidOutbox;
        this.deterministicIdGenerator = deterministicIdGenerator;
        this.stringHasher = stringHasher;
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

        if (isExchangeTokenRequestDuplicate(exchangeTokenRequestDto)) {
            throw new DuplicateConnectionException("This connection already exists. Remove the current one before continue.");
        }

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
                new ExchangeTokenRequestDto(response.getPublicToken(), Collections.emptyList(), Collections.emptyList(),
                        institutionId, "Sandbox Bank"),
                userId);
    }

    @Override
    @Transactional
    @Observed(name = "plaid.remove-connection", contextualName = "plaid remove connection")
    public void removeConnection(UUID connectionId, String reason) {
        Connection connection = connectionService.readByIdForUpdate(connectionId);
        if (!connection.isActive()) {
            return;
        }

        long syncVersion = connectionService.incrementSyncVersion(connection);

        plaidClient.removeItem(new ItemRemoveRequest().accessToken(connection.getAccessToken()));

        connectionService.deactivate(connectionId, reason);
        plaidOutbox.connectionRemoved(new ConnectionRemoved(
                connection.getConnectionId(),
                connection.getUserId(),
                reason), syncVersion);
    }

    @Override
    @Transactional
    @Observed(name = "plaid.sync-connection", contextualName = "plaid sync connection")
    @Retryable(retryFor = PlaidRetryableException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 60000))
    public void syncConnection(UUID connectionId) {
        Connection conn = connectionService.readByIdForUpdate(connectionId);
        if (!conn.isActive()) {
            return;
        }

        String lastCursor = conn.getLastCursor();
        long syncVersion = connectionService.incrementSyncVersion(conn);

        Set<AccountBase> accounts = new HashSet<>();
        List<Transaction> transactions = new ArrayList<>();

        TransactionsSyncRequest request = new TransactionsSyncRequest()
                .accessToken(conn.getAccessToken())
                .cursor(lastCursor)
                .options(new TransactionsSyncRequestOptions().includePersonalFinanceCategory(true));

        TransactionsSyncResponse response = plaidClient.syncTransactions(request);

        accounts.addAll(response.getAccounts());
        transactions.addAll(response.getAdded());
        transactions.addAll(response.getModified());

        lastCursor = response.getNextCursor();
        boolean hasMore = response.getHasMore();

        while (hasMore) {
            request.setCursor(lastCursor);
            response = plaidClient.syncTransactions(request);

            accounts.addAll(response.getAccounts());
            transactions.addAll(response.getAdded());
            transactions.addAll(response.getModified());

            lastCursor = response.getNextCursor();
            hasMore = response.getHasMore();
        }

        for (AccountBase account : accounts) {
            UUID accountId = deterministicIdGenerator.accountId(conn.getUserId(), account.getAccountId());
            String fingerprint = accountFingerprint(conn, account);
            accountRegistryService.registerAccount(
                    connectionId,
                    accountId,
                    fingerprint);

            AccountObserved accountObserved = plaidRequestMapper.toAccountObserved(
                    conn,
                    accountId,
                    account);
            plaidOutbox.accountObserved(accountObserved, syncVersion);
        }

        for (Transaction transaction : transactions) {
            UUID accountId = deterministicIdGenerator.accountId(conn.getUserId(), transaction.getAccountId());
            UUID transactionId = deterministicIdGenerator.transactionId(
                    conn.getUserId(),
                    transaction.getAccountId(),
                    transaction.getTransactionId());

            TransactionObserved transactionObserved = plaidRequestMapper.toTransactionObserved(
                    conn,
                    transactionId,
                    accountId,
                    transaction);
            plaidOutbox.transactionObserved(transactionObserved, syncVersion);
        }

        connectionService.completeSync(connectionId, lastCursor);

    }

    private boolean isExchangeTokenRequestDuplicate(ExchangeTokenRequestDto request) {
        if (request == null || request.getAccounts() == null || request.getAccounts().isEmpty()) {
            return false;
        }

        String normalizedInstitutionId = request.getInstitutionId() == null
                ? ""
                : request.getInstitutionId().trim().toLowerCase(Locale.ROOT);

        List<String> fingerprints = request.getAccounts().stream()
                .map(account -> {
                    String normalizedMask = account.getMask() == null
                            ? ""
                            : account.getMask().trim().toLowerCase(Locale.ROOT);

                    String normalizedSubType = account.getSubType() == null
                            ? ""
                            : account.getSubType().trim().toLowerCase(Locale.ROOT);

                    String valueToHash = String.join("|",
                            normalizedMask,
                            normalizedSubType,
                            normalizedInstitutionId
                    );

                    return stringHasher.sha256(valueToHash);
                })
                .toList();

        return accountRegistryService.existsByFingerprintIn(fingerprints);
    }

    private String accountFingerprint(Connection connection, AccountBase account) {
        String normalizedMask = account.getMask() == null ? "" : account.getMask().trim().toLowerCase(Locale.ROOT);
        String normalizedSubType = account.getSubtype() == null ? "" : account.getSubtype().getValue().trim().toLowerCase(Locale.ROOT);
        String normalizedInstitutionId = connection.getInstitutionId() == null
                ? ""
                : connection.getInstitutionId().trim().toLowerCase(Locale.ROOT);

        return stringHasher.sha256(String.join("|", normalizedMask, normalizedSubType, normalizedInstitutionId));
    }

}
