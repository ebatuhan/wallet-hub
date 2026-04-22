package com.batu.consent_service.provider.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.consent_service.client.PlaidAdapterClient;
import com.batu.consent_service.provider.ProviderConnectionGateway;
import com.batu.shared.dto.request.ConnectionExchangeRequestDto;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@Component
public class PlaidConnectionGateway implements ProviderConnectionGateway {

    private static final String PLAID = "PLAID";

    private final PlaidAdapterClient plaidAdapterClient;

    public PlaidConnectionGateway(PlaidAdapterClient plaidAdapterClient) {
        this.plaidAdapterClient = plaidAdapterClient;
    }

    @Override
    public String provider() {
        return PLAID;
    }

    @Override
    public LinkTokenResponseDto createLinkToken(UUID userId, String country) {
        return plaidAdapterClient.createLinkToken(userId, new LinkTokenRequestDto(country)).getBody();
    }

    @Override
    public ExchangeTokenResponseDto exchangeConnection(UUID userId, ConnectionExchangeRequestDto request) {
        return plaidAdapterClient.exchangeToken(
                userId,
                new ExchangeTokenRequestDto(
                        request.getPublicToken(),
                        request.getAccountIds(),
                        request.getInstitutionId(),
                        request.getInstitutionName()))
                .getBody();
    }

    @Override
    public ExchangeTokenResponseDto mockConnection(UUID userId) {
        return plaidAdapterClient.mockToken(userId).getBody();
    }

    @Override
    public ConnectionResponseDto getConnection(UUID userId, UUID providerConnectionId) {
        return plaidAdapterClient.getConnection(userId, providerConnectionId).getBody();
    }

    @Override
    public ConnectionResponseDto updateConnection(UUID userId, UUID providerConnectionId, ConnectionUpdateRequestDto request) {
        return plaidAdapterClient.updateConnection(userId, providerConnectionId, request).getBody();
    }

    @Override
    public void refreshConnection(UUID userId, UUID providerConnectionId) {
        plaidAdapterClient.refreshConnection(userId, providerConnectionId);
    }

    @Override
    public void removeConnection(UUID userId, UUID providerConnectionId) {
        plaidAdapterClient.removeConnection(userId, providerConnectionId);
    }
}
