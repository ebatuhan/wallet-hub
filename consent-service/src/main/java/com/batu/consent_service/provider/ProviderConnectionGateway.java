package com.batu.consent_service.provider;

import java.util.UUID;

import com.batu.shared.dto.request.ConnectionExchangeRequestDto;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

public interface ProviderConnectionGateway {

    String provider();

    LinkTokenResponseDto createLinkToken(UUID userId, String country);

    ExchangeTokenResponseDto exchangeConnection(UUID userId, ConnectionExchangeRequestDto request);

    ExchangeTokenResponseDto mockConnection(UUID userId);

    ConnectionResponseDto getConnection(UUID userId, UUID providerConnectionId);

    ConnectionResponseDto updateConnection(UUID userId, UUID providerConnectionId, ConnectionUpdateRequestDto request);

    void refreshConnection(UUID userId, UUID providerConnectionId);

    void removeConnection(UUID userId, UUID providerConnectionId);
}
