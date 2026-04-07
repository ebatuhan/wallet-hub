package com.batu.plaid_adapter_service.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

public interface LinkService {
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal);

    ExchangeTokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal);

    ExchangeTokenResponseDto mockToken(Jwt principal);

}
