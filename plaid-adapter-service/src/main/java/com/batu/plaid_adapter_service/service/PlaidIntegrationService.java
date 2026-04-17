package com.batu.plaid_adapter_service.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

public interface PlaidIntegrationService {
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal);

    ExchangeTokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal);

    ExchangeTokenResponseDto mockToken(Jwt principal);

    void syncAccountsAndTransactions(Connection connection);

    void deactivateConnectionData(Connection connection);

}
