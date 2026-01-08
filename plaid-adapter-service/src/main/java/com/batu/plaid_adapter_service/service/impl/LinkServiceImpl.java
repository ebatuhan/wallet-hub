package com.batu.plaid_adapter_service.service.impl;

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
import com.batu.plaid_adapter_service.service.LinkService;
import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;

import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateRequestOptions;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;

@Service
public class LinkServiceImpl implements LinkService {

    @Value("${plaid.webhook.url:}")
    private String webhookUrl;

    private final PlaidClientWrapper plaidClient;
    private final ConnectionService connectionService;

    public LinkServiceImpl(PlaidClientWrapper plaidClient, ConnectionService connectionService) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
    }

    @Override
    @Retryable(
        retryFor = {PlaidRetryableException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
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
    @Retryable(
        retryFor = {PlaidRetryableException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExhcangetokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal) {
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

        return new ExhcangetokenResponseDto(
                savedConnection.getInstitutionId(),
                savedConnection.getInstitutionName());
    }

    @Override
    @Retryable(
        retryFor = {PlaidRetryableException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExhcangetokenResponseDto mockToken(Jwt principal) {
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
}