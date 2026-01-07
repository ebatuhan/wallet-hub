package com.batu.plaid_adapter_service.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;

public interface LinkService {
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal);

    ExhcangetokenResponseDto exchangeToken(ExchangeTokenRequestDto exchangeTokenRequestDto, Jwt principal);

    ExhcangetokenResponseDto mockToken(Jwt principal);

}
