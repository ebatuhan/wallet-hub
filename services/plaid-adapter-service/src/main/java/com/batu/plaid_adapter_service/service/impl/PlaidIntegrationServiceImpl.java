package com.batu.plaid_adapter_service.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.micrometer.observation.annotation.Observed;

import com.batu.plaid_adapter_service.client.AccountClient;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.client.TransactionClient;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
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
    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final ConnectionService connectionService;
    private final PlaidRequestMapper plaidRequestMapper;
    private final DeterministicIdGenerator deterministicIdGenerator;

    public PlaidIntegrationServiceImpl(PlaidClientWrapper plaidClient,
            AccountClient accountClient,
            TransactionClient transactionClient,
            ConnectionService connectionService,
            PlaidRequestMapper plaidRequestMapper,
            DeterministicIdGenerator deterministicIdGenerator) {
        this.plaidClient = plaidClient;
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
        this.connectionService = connectionService;
        this.plaidRequestMapper = plaidRequestMapper;
        this.deterministicIdGenerator = deterministicIdGenerator;
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

        if (exchangeTokenRequestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exchange token request is required");
        }

        if (isExchangeTokenRequestDuplicate(exchangeTokenRequestDto, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This connection already exists. Remove the current one before continue.");
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

        connectionService.deactivate(connectionId, reason);
        accountClient.deactivateAccountsByConnection(connection.getConnectionId());
        plaidClient.removeItem(new ItemRemoveRequest().accessToken(connection.getAccessToken()));
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

            accountClient.upsertAccount(plaidRequestMapper.toAccountUpsertRequest(
                    conn,
                    accountId,
                    account));
        }

        for (Transaction transaction : transactions) {
            UUID accountId = deterministicIdGenerator.accountId(conn.getUserId(), transaction.getAccountId());
            UUID transactionId = deterministicIdGenerator.transactionId(
                    conn.getUserId(),
                    transaction.getAccountId(),
                    transaction.getTransactionId());

            transactionClient.upsertTransaction(plaidRequestMapper.toTransactionUpsertRequest(
                    conn,
                    transactionId,
                    accountId,
                    transaction));
        }

        connectionService.completeSync(connectionId, lastCursor);

    }

    private boolean isExchangeTokenRequestDuplicate(ExchangeTokenRequestDto request, UUID userId) {
        if (request == null || request.getAccounts() == null || request.getAccounts().isEmpty()) {
            return false;
        }

        List<Connection> existingConnections = connectionService.readAllByUserIdAndInstitutionId(userId, request.getInstitutionId());

        for (Connection connection : existingConnections) {
            List<AccountResponseDto> existingAccounts = accountClient.findAccountsByConnectionId(connection.getConnectionId());
            for (AccountResponseDto existingAccount : existingAccounts) {
                boolean duplicate = request.getAccounts().stream().anyMatch(incomingAccount ->
                        normalize(incomingAccount.getMask()).equals(normalize(existingAccount.getAccountMask()))
                                && normalize(incomingAccount.getSubType()).equals(normalize(existingAccount.getAccountSubtype())));
                if (duplicate) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

}
