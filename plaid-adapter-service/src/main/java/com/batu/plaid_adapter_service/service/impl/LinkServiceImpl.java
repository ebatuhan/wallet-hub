package com.batu.plaid_adapter_service.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.LinkService;
import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;
import com.google.gson.Gson;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.PlaidError;


@Service
public class LinkServiceImpl implements LinkService {

    @Value("${plaid.webhook.url:}")
    private String webhookUrl;

    private final PlaidApi plaidClient;
    private final ConnectionService connectionService;

    public LinkServiceImpl(PlaidApi plaidClient, ConnectionService connectionService) {
        this.plaidClient = plaidClient;
        this.connectionService = connectionService;
    }

    @Override
    public LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal) {
        var request = new LinkTokenCreateRequest()
                .userId(principal.getSubject())
                .clientName("Wallet-Hub")
                .language("en")
                .countryCodes(List.of(com.plaid.client.model.CountryCode.US))
                .products(List.of(Products.TRANSACTIONS))
                .webhook(webhookUrl);

        try {
            var response = plaidClient.linkTokenCreate(request).execute();

            var body = response.body();
            if (response.isSuccessful() && body != null) {
                String linkToken = body.getLinkToken();
                return new LinkTokenResponseDto(linkToken);
            }

            else {
                var errorBody = response.errorBody();

                if (errorBody != null) {
                    Gson gson = new Gson();
                    PlaidError plaidError = gson.fromJson(errorBody.string(), PlaidError.class);

                    String displayMessage = plaidError.getDisplayMessage() != null
                            ? plaidError.getDisplayMessage()
                            : "An error occurred in Plaid";

                    throw new PlaidClientException(displayMessage, HttpStatus.BAD_GATEWAY);
                }

                throw new PlaidClientException("Plaid sent empty error", HttpStatus.BAD_GATEWAY);
            }

        } catch (IOException ex) {
            throw new PlaidClientException("Unable to connect to banking provider.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ExhcangetokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal) {

        var request = new ItemPublicTokenExchangeRequest()
                .publicToken(exchangeTokenRequestDto.getPublicToken());

        try {
            var response = plaidClient
                    .itemPublicTokenExchange(request)
                    .execute();

            var body = response.body();

            if (body != null && response.isSuccessful()) {

                UUID userId = UUID.fromString(principal.getSubject());

                Connection connection = new Connection(
                        userId,
                        body.getItemId(),
                        body.getAccessToken(),
                        exchangeTokenRequestDto.getInstitutionId(),
                        exchangeTokenRequestDto.getInstitutionName());

                Connection savedConnection = connectionService.create(connection);

                return new ExhcangetokenResponseDto(savedConnection.getInstitutionId(),
                        savedConnection.getInstitutionName());
            }

            else {
                var errorBody = response.errorBody();

                if (errorBody != null) {
                    Gson gson = new Gson();

                    PlaidError plaidError = gson.fromJson(errorBody.string(), PlaidError.class);
                    String displayMessage = plaidError.getDisplayMessage() != null
                            ? plaidError.getDisplayMessage()
                            : "An error occurred in Plaid";

                    throw new PlaidClientException(displayMessage, HttpStatus.BAD_GATEWAY);
                }
                throw new PlaidClientException("Plaid sent empty error", HttpStatus.BAD_GATEWAY);
            }
        }

        catch (IOException ex) {
            throw new PlaidClientException("Unable to connect to banking provider.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}