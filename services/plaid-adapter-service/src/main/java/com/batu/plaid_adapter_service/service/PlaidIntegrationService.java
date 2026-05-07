package com.batu.plaid_adapter_service.service;

import java.util.UUID;

import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

public interface PlaidIntegrationService {
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, UUID userId);

    ExchangeTokenResponseDto exchangeLinkToken(ExchangeTokenRequestDto exchangeTokenRequestDto, UUID userId);

    ExchangeTokenResponseDto mockToken(UUID userId);

    void syncConnection(UUID connectionId);

    void removeConnection(UUID connectionId, String reason);

}
